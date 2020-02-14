# Development documentation

## Linters

Linting is an important part of keeping your codebase clean and correct. Unfortunately, no current linting tool covers
all the needs we have, so we use a combination of them:

- The compiler itself has some interesting linting options. They are enabled in this build. Many of those are just
warnings, but having a warning not fail the build makes you accumulate more and more warnings. So we make warnings
fail the build.

- [WartRemover](https://www.wartremover.org/) is one of the more powerful current linters for scala. It works as a
compiler plugin, which means the compiler will issue the warnings. Developing new rules implies knowledge about the 
compiler internals, but it has a comprehensive list of lint rules that are very interesting.

- [Scalafix](https://scalacenter.github.io/scalafix) is probably the lint tool of the future. It not only allows you to
validate your bild, but it also allows you to fix it automatically. Writing linters for scalafix does not involve 
messign with the compiler internals, so better and better scalafix rules are expected to appear in the foreseeable 
future. It does not run as part of the compilation but as a different, post-compilation verification. In particular, it 
is the only of our available tools that is able to [sort imports](https://github.com/NeQuissimus/sort-imports). 

### Supressing false negatives

There are occasions when you need to play out of the rules. They should be exceptional, like when you write a helper 
`===` function that you'll use instead of universal equality and you implement it in terms of `==`. When this happens
you need to supress the warning the linter emmits, effectively telling it that it is ok to use equals (or null, or 
throw, etc) there.

Unfortunately, suppressing negatives is done differently depending on which linter is complaining:

 - To suppress warnings issued by the compiler (including wart remover) you can use `@silent("<pattern>")` where 
 `<pattern>` is a regex of the message you want to suppress. Unfortunately you can't use constants for the contents
 of the annotation; you must use a string literal.
 
 - To suppress scalafix warnings, you can use `@SuppressWarnings(Array("scalafix:<rule1>", "scalafix:<rule2>"))` 
 where `ruleN` is the name of the scalafix rule you want to suppress. You can get the name of the rule from the error
 message you want to suppress, where it appears between square brackets (e.g. `error: [DisableSyntax.null] null should 
 be avoided`) 
 
