package com.agilogy.rosetta.caliban

import cats.implicits._

import caliban.CalibanError.ExecutionError
import caliban.Value.NullValue
import caliban.introspection.adt.__Field
import caliban.schema.{ ArgBuilder, GenericSchema, Schema, Step }
import caliban.{ schema, CalibanError, InputValue }

import com.agilogy.rosetta.engine.Engine
import com.agilogy.rosetta.read.ReadErrorCause.NativeReadError
import com.agilogy.rosetta.read.{ NativeRead, ReadError, Segment }
import com.agilogy.rosetta.write.NativeWrite

object CalibanEngine extends Engine[ArgBuilder, InputValue, ExecutionError, Schema.Typeclass, Step[Any]] {

  override implicit def nativeWriteInstance: NativeWrite[schema.Schema.Typeclass] = new NativeWrite[Schema.Typeclass] {
    override def nativeObjectWriter[A](
      name: String,
      attributes: List[(String, Schema.Typeclass[A])]
    ): Schema.Typeclass[A] = {
      val schema = new GenericSchema[Any] {}
      val objectFields = attributes.map {
        case (fieldName, fieldSchema) =>
          __Field(fieldName, None, fieldSchema.arguments, () => fieldSchema.toType(), isDeprecated = false, None) ->
            (b => fieldSchema.resolve(b))
      }
      schema.objectSchema[Any, A](name, None, objectFields)
    }
    override def contramap[A, B](fa: Schema.Typeclass[A])(f: B => A): Schema.Typeclass[B] = fa.contramap(f)
  }

  override def writeNative[A: Schema.Typeclass](value: A): Step[Any] = implicitly[Schema.Typeclass[A]].resolve(value)

  override def listNativeWrite[A: Schema.Typeclass]: Schema.Typeclass[List[A]] = Schema.listSchema[A]

  override def optionalNativeWrite[A: _root_.caliban.schema.Schema.Typeclass]: Schema.Typeclass[Option[A]] =
    Schema.optionSchema[A]

  override def readNative[A: ArgBuilder](input: InputValue): Either[ReadError, A] =
    implicitly[ArgBuilder[A]]
      .build(input)
      .leftMap(error => ReadError(NativeReadError(error), error.fieldName.map(Segment.Attribute).toList))

  override def listNativeRead[A: ArgBuilder]: ArgBuilder[List[A]] = ArgBuilder.list[A]

  override def optionalAttributeNativeRead[A: ArgBuilder](attributeName: String): ArgBuilder[Option[A]] =
    new ArgBuilder[Option[A]] {
      override def build(input: InputValue): Either[ExecutionError, Option[A]] = input match {
        case InputValue.ObjectValue(fields) =>
          fields
            .getOrElse(attributeName, NullValue)
            .asRight[ExecutionError]
            .flatMap(input => ArgBuilder.option[A].build(input))
        case _ => ExecutionError(s"Missing field $attributeName").asLeft
      }
    }

  override def attributeNativeRead[A: ArgBuilder](attributeName: String): ArgBuilder[A] = new ArgBuilder[A] {
    override def build(input: InputValue): Either[ExecutionError, A] = input match {
      case InputValue.ObjectValue(fields) =>
        fields
          .getOrElse(attributeName, NullValue)
          .asRight[ExecutionError]
          .flatMap(input => implicitly[ArgBuilder[A]].build(input))
      case _ => ExecutionError(s"Missing field $attributeName").asLeft
    }
  }

  override implicit def nativeReadInstance: NativeRead[ArgBuilder, ExecutionError] =
    new NativeRead[ArgBuilder, ExecutionError] {
      override def andThen[A, B](nativeRead: ArgBuilder[A])(f: A => Either[ExecutionError, B]): ArgBuilder[B] =
        nativeRead.build(_).flatMap(f)
      override def leftMap[A](nativeRead: ArgBuilder[A])(f: ExecutionError => ExecutionError): ArgBuilder[A] =
        nativeRead.build(_).leftMap(f)

      override def product[A, B](fa: ArgBuilder[A], fb: ArgBuilder[B]): ArgBuilder[(A, B)] = new ArgBuilder[(A, B)] {
        override def build(input: InputValue): Either[CalibanError.ExecutionError, (A, B)] =
          (fa.build(input), fb.build(input)).tupled
      }

    }
}
