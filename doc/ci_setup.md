# Elastic Beanstalk for Link microservices

Tools needed:

* awscli: Install following any method on https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html
* ebcli: It may be optional: https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3-install.html

IAM policies needed:
* `AWSElasticBeanstalkFullAccess`


## Publish a docker image

Service are dockerized with sbt native packager. This generates a docker image that need to be pushed to a repository.

We decided to use aws ecr, because it was the easiest to integrate later on.


To create a ecr repository. This is only needed the firs time:
```
aws ecr create-repository --repository-name link-microservices --region eu-west-1
```

The following steps are already automatized on the pipeline and don't need to be run locally.

To login to the registry:
```
eval $(aws ecr get-login --no-include-email | sed 's;https://;;g')
```

To publish the image:
```
sbt example/docker:publish
```

### Create an application on beanstalk

On https://eu-west-1.console.aws.amazon.com/elasticbeanstalk/home?region=eu-west-1 or through `eb`

Create an environment selecting name and url.

We selected the type  `WebService` with the platform `Multi-container Docker`

On the code, you can deploy the example app because it will be replaced with the actual one on the first deploy.

### Continuous deployment

As we can see on https://bitbucket.org/smartcitylink/link-microservices/src/master/bitbucket-pipelines.yml the pipeline is already configured to deploy to environments `link-microservices-prod` and `link-microservices-staging`.

The interesting parts on this file are:

```
- envsubst < Dockerrun.aws.tpl.json > ${S3_FILE}
- aws s3 cp ${S3_FILE} s3://${S3_BUCKET}/${S3_FILE}
- aws elasticbeanstalk create-application-version --application-name link-microservices --source-bundle S3Bucket=${S3_BUCKET},S3Key=${S3_FILE} --version-label=${VERSION}
# Trigger deploy of the new version
- aws elasticbeanstalk update-environment --environment-name link-microservices-${ENVIRONMENT} --version-label=${VERSION}
```

On this steps we generate a file `Dockerrun.aws.json` containing the link to the docker image that has been published previously. Then we create a new version for the application using the image and we apply the change with the update environment command.

There's an additional step required that is adding the policy `AmazonEC2ContainerRegistryReadOnly` to the role `aws-elasticbeanstalk-ec2-role`.
This is needed so the ec2 instance can retrieve the docker image from the ecr repository.

### Bitbucket pipelines info

Bitbucket pipelines can run any docker image published on docker hub. We cooked a custom image that contains sbt and aws so it includes the needed tools. 
This image is available at `agilogy/sbt-docker-awscli`