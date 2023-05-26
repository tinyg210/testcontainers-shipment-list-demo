
# Shipment List Demo Application - AWS in PROD and LocalStack on DEV environment


| Environment      | <img src="https://img.shields.io/badge/LocalStack-deploys-4D29B4.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAKgAAACoABZrFArwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAALbSURBVHic7ZpNaxNRFIafczNTGIq0G2M7pXWRlRv3Lusf8AMFEQT3guDWhX9BcC/uFAr1B4igLgSF4EYDtsuQ3M5GYrTaj3Tmui2SpMnM3PlK3m1uzjnPw8xw50MoaNrttl+r1e4CNRv1jTG/+v3+c8dG8TSilHoAPLZVX0RYWlraUbYaJI2IuLZ7KKUWCisgq8wF5D1A3rF+EQyCYPHo6Ghh3BrP8wb1en3f9izDYlVAp9O5EkXRB8dxxl7QBoNBpLW+7fv+a5vzDIvVU0BELhpjJrmaK2NMw+YsIxunUaTZbLrdbveZ1vpmGvWyTOJToNlsuqurq1vAdWPMeSDzwzhJEh0Bp+FTmifzxBZQBXiIKaAq8BBDQJXgYUoBVYOHKQRUER4mFFBVeJhAQJXh4QwBVYeHMQJmAR5GCJgVeBgiYJbg4T8BswYPp+4GW63WwvLy8hZwLcd5TudvBj3+OFBIeA4PD596nvc1iiIrD21qtdr+ysrKR8cY42itCwUP0Gg0+sC27T5qb2/vMunB/0ipTmZxfN//orW+BCwmrGV6vd63BP9P2j9WxGbxbrd7B3g14fLfwFsROUlzBmNM33XdR6Meuxfp5eg54IYxJvXCx8fHL4F3w36blTdDI4/0WREwMnMBeQ+Qd+YC8h4g78wF5D1A3rEqwBiT6q4ubpRSI+ewuhP0PO/NwcHBExHJZZ8PICI/e73ep7z6zzNPwWP1djhuOp3OfRG5kLROFEXv19fXP49bU6TbYQDa7XZDRF6kUUtEtoFb49YUbh/gOM7YbwqnyG4URQ/PWlQ4ASllNwzDzY2NDX3WwioKmBgeqidgKnioloCp4aE6AmLBQzUExIaH8gtIBA/lFrCTFB7KK2AnDMOrSeGhnAJSg4fyCUgVHsolIHV4KI8AK/BQDgHW4KH4AqzCQwEfiIRheKKUAvjuuu7m2tpakPdMmcYYI1rre0EQ1LPo9w82qyNziMdZ3AAAAABJRU5ErkJggg=="> <img src="https://img.shields.io/badge/AWS-deploys-F29100.svg?logo=amazon"> |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| __Services__     | Amazon S3, Lambda, DynamoDB, SNS, SQS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| __Integrations__ | AWS SDK, Terraform, AWS CLI                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| __Categories__   | Spring Boot, S3 Trigger                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| __Level__        | Intermediate                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| __Works on__     | LocalStack v2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |


## Introduction

This application was created for demonstration purposes to highlight the ease of switching from
using actual AWS dependencies to having them emulated on LocalStack for your *developer environment* .
Of course this comes with other advantages, but the first focus point is making the transition.

## Architecture Overview

![Diagram](app_diagram.png)

## Prerequisites

- [Maven 3.8.5](https://maven.apache.org/install.html) & [Java 17](https://www.java.com/en/download/help/download_options.html)
- [AWS free tier account](https://aws.amazon.com/free/)
- [LocalStack](https://localstack.cloud/)
- [Docker](https://docs.docker.com/get-docker/) - for running LocalStack
- [Terraform](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli) (+ Python pip for [tflocal](https://pypi.org/project/terraform-local/)) for creating AWS & LocalStack resources
- [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) - for running the frontend app

#### What it does

*shipment-list-demo* is a Spring Boot application dealing with CRUD operations an employee can
execute
on a bunch of shipments that they're allowed to view - think of it like the Post app.
The demo consists of a backend and a frontend implementation, using React to display the
information.
The AWS services involved are:

- [S3](https://docs.localstack.cloud/user-guide/aws/s3/) for storing pictures
- [DynamoDB](https://docs.localstack.cloud/user-guide/aws/dynamodb/) for the entities
- [Lambda](https://docs.localstack.cloud/user-guide/aws/lambda/) function that will validate the pictures, apply a watermark and replace non-compliant files.
- [SNS](https://docs.localstack.cloud/user-guide/aws/sns/) that receives update notifications
- [SQS](https://docs.localstack.cloud/user-guide/aws/sqs/) that subscribes to a topic and delivers the messages to the Spring Boot app


#### How to use it

We’ll be walking through a few scenarios using the application, and we expect it to maintain the
behavior in both production and development environments. This behaviour can be "scientifically"
backed up
by adding integration tests.

We’ll take advantage of one of the core features of the Spring framework that allows us to bind our
beans to different profiles, such as dev, test, and prod. Of course, these beans need to know how to
behave in each environment, so they’ll get that information from their designated configuration
files, `application-prod.yml`, and  `application-dev.yml`.

## Instructions

## Running on AWS

Now, we don’t have a real production environment because that’s not the point here, but most likely,
an application like this runs on a container orchestration platform, and all the necessary configs
are still provided. Since we’re only simulating a production instance, all the configurations are
kept in the `application-prod.yml` file.

### User credentials

Before getting started, it's important to note that an IAM user, who's credentials will be used,
needs to be created with the following policies:

- AmazonS3FullAccess
- AWSLambda_FullAccess
- AmazonDynamoDBFullAccess
- AmazonSNSFullAccess
- AmazonSQSFullAccess
- AWSLambdaExecute
- AmazonS3ObjectLambdaExecutionRolePolicy

We will be using the user's credentials and export them as temporary environment variables with the
`export` (`set` on Windows) command:

```
$ export AWS_ACCESS_KEY_ID=[your_aws_access_key_id]
$ export AWS_SECRET_ACCESS_KEY=[your_aws_secret_access_key_id]
```

### Building the validator module

Step into the `shipment-picture-lambda-validator` module and run `mvn clean package shade:shade`.
This will create an uber-jar by packaging all its dependencies. We'll need this one in the next
step.

### Creating resources - running Terraform

Make sure you have Terraform [installed](https://developer.hashicorp.com/terraform/downloads)

Under setup/terraform run:

```
$ terraform init
$ terraform plan
```

Once these 2 commands run successfully and no errors occur, it's time to run:

```
$ terraform apply
```

This should create the needed S3 bucket, the DynamoDB `shipment` table and populate it with some
sample data, the Lambda function that will help with picture validation, the SQS and SNS.

### Running the GUI

Now `cd` into `src/main/shipment-list-frontend` and run `npm install` and `npm start`.
This will spin up the React app that can be accessed on `localhost:3000`.

For running it on Windows, there are some
[extra requirements](https://learn.microsoft.com/en-us/windows/dev-environment/javascript/react-on-windows)
,
but no worries, it should be straightforward.

Go back to the root folder and run the backend simply by using

```
$ mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Notice the `prod` profile is being set via command line arguments.

### Using the application

At `localhost:3000` you should now be able to see a list of shipments with standard icons,
that means that only the database is populated, the pictures still need to be added from the
`sample-pictures` folder.

The weight of a shipment is already given, but not the size, that's why we need pictures to
understand it better,
using the "banana for scale" measuring unit. How else would we know??

Current available actions using the GUI:

- upload a new image
- delete shipment from the list
- create and update shipment are available only via Postman (or any other API platform)

Files that are not pictures will be deleted
and the shipment picture will be replaced with a generic icon, because we don't want any trouble.

## Running on LocalStack


To switch to using LocalStack instead of AWS services just run `docker compose up` in the root
folder
to spin up a Localstack container.

Before we proceed, make sure you clean up your AWS resources by running

```
$ terraform destroy
```

To generate the exact same resources on LocalStack, we need `tflocal`, a thin wrapper script around
the terraform command line client. `tflocal` takes care of automatically configuring the local
service
endpoints, which allows you to easily deploy your unmodified Terraform scripts against LocalStack.

You can [install](https://docs.localstack.cloud/user-guide/integrations/terraform/) the `tflocal`
command via pip (requires a local Python installation):

```
$ pip install terraform-local
```

Once installed, the `tflocal` command should be available, with the same interface as the terraform
command line. Try it out:

```
$ tflocal --help
Usage: terraform [global options] <subcommand> [args]
...
```

From here on, it's smooth sailing, the same as before. In the `setup/terraform` folder, run the `cleanup` script
to get rid of any files that keep track of the resources' state. Then:

```
$ tflocal init
$ tflocal plan -var 'env=dev
$ tflocal apply
```

What we're doing here is just passing an environmental variable to let the Lambda
know this is the `dev` environment.

After that, the Spring Boot application needs to start using the dev profile (make sure you're in
the
root folder):

```
$ mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Go back to `localhost:3000` and a new list will be available; notice that the functionalities of
the application have not changed.

There you have it, smooth transition from AWS to Localstack, with no code change. 👍🏻


