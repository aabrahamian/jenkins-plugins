# jenkins-plugins

Specialized Jenkins plugins for [Affirm]. Each subdirectory should be one plugin.

## Developing

Refer to [Plugin Tutorial] and configure `~/.m2/settings.xml`.

To create a new plugin use

```sh
mvn -U hpi:create
```

To run tests and build a plugin package, use

```sh
mvn package
```

If you are impatient, use offline mode and skip tests:

```
mvn -o -DskipTests package
```

In IntelliJ

1. Import `pom.xml` from the subdirectory you want to work on.
1. Create a new Run Configuration for Maven. Set "Command line:" to `hpi:run`.
1. In Project Settings \> Project and Project Settings \> Modules, ensure that the
   Project SDK and Language Level are both set to 1.8.

Use the Run Configuration to start a Jenkins server locally using "Run" or "Debug".

To make builds slightly faster, configure Maven to work offline in Preferences.

## Testing

See [Using JUnit].

## Releasing

TBD

  [Affirm]: https://www.affirm.com/
  [Plugin Tutorial]: https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial
  [Using JUnit]: http://maven.apache.org/surefire/maven-surefire-plugin/examples/junit.html
