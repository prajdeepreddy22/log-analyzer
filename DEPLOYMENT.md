# LogAI Backend Deployment

## Critical post-EB-environment step

After the Elastic Beanstalk environment is created and before the application
starts, you must manually add an inbound rule to the RDS security group:

```text
Type: MySQL/Aurora
Protocol: TCP
Port: 3306
Source: the security group attached to the EB EC2 instances (not an IP range)
```

Without this rule the application will crash at startup with a database
connection error. This cannot be automated via EB configuration because the EB
EC2 security group ID is only known after environment creation.

## CORS configuration after CloudFront

After the CloudFront distribution is created and its domain is known, for
example `https://dXXXXXXXXXXXX.cloudfront.net`, update the Elastic Beanstalk
environment variable:

```text
CORS_ALLOWED_ORIGINS=https://dXXXXXXXXXXXX.cloudfront.net
```

Then trigger an Elastic Beanstalk environment restart. Until this is set
correctly, browser requests for AI analysis and chat will fail with CORS errors.

## CloudFront S3 origin — use website endpoint

When configuring the CloudFront distribution's S3 origin for the Angular
frontend, use the S3 static website hosting endpoint, not the S3 REST endpoint.

```text
Correct: your-bucket-name.s3-website.ap-south-1.amazonaws.com
Wrong:   your-bucket-name.s3.amazonaws.com
```

Using the REST endpoint breaks Angular SPA routing. Deep links return a 403 or
404 from S3 instead of serving `index.html`.

## CloudFront SSE timeout

CloudFront's default origin response timeout is 60 seconds, which can cut SSE
streams off mid-response. The LogAI SSE endpoint supports responses lasting up
to 300 seconds.

Manually set the CloudFront origin response timeout to 300 seconds in the AWS
console for the origin used by the `/api/*` cache behavior. This setting cannot
be configured through Elastic Beanstalk `.ebextensions`.
