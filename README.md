# faas
Lambda Function:This project is designed to trigger mail to reset password for users

## Getting Started

### Prerequisites
* Git
* JDK 8 or later
* Maven 3.0 or later

### Clone
To get started you can simply clone this repository using git:
```
git clone git@github.com:patroa-su2020/faas.git
```

### Build an executable JAR
You can run the application from the command line using:
```
mvn clean install
```
Or you can build a single executable JAR file that contains all the necessary dependencies, classes, and resources with:
```
mvn clean package
```
Then upload the jar file to S3 Bucket


*Instead of `mvn` you can also use the maven-wrapper `./mvnw` to ensure you have everything necessary to run the Maven build.*


## CI/CD
```$xslt
1 Trigger Circle CI build
2 The script builds the project and uploads the jar to s3 bucket
3 aws_lambda_function resource created through terraform make use of this jar file as a microservice
4 any payload on the SNS topic will trigger the lambda function
```

#How the lambda function works?
```
1 Once the user clicks on forgot password link:
2 A message(user email) is put on Amazon SNS topic
3 Lambda function subscribed to that topic, fetches the email address
4 Triggers an email to the desired address with a unique password reset link
```


This project is licensed under the terms of the [MIT license](LICENSE).

##Application Endpoints to access lambda function
```
http://<domain_name>/forgotpassword
```