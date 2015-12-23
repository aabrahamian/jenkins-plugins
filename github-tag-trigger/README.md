# github-tag-trigger

## Usage

1. Setup github-plugin as usual.
1. Create a new project using the Git SCM.
1. Under "Advanced", use `+refs/tags/*:refs/remotes/origin/tags/*`.
1. Under "Branches to build", use `${GIT_REF}`.
1. Check "Build when a tag is edited on GitHub."

### Manual Triggers

To allow manual triggering, add a string parameter named `GIT_REF` that
defaults to `refs/heads/master`.

## Developing

Refer to instructions from the top-level README.md. After starting the server,
I use Postman to send a webhook to `http://localhost:8080/jenkins/github-webhook/`
for a fake repository I created on GitHub.

Note that you don't have to restart the Jenkins server when you modify
`*.jelly` files. See [Basic Guide to Jelly].

## Testing

To build the hpi file and run tests, use

```bash
mvn -o package
```

## Releasing

TBD

  [Basic Guide to Jelly]: https://wiki.jenkins-ci.org/display/JENKINS/Basic+guide+to+Jelly+usage+in+Jenkins
