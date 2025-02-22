# Eclipse GMF Runtime

**NOTE:** As of september of 2021, the source code for GMF Runtime is now hosted on GitHub at https://github.com/eclipse/gmf-runtime. If you have Git clones which refers to the old location at git.eclipse.org, update them or you will not get the latest changes.

The [Eclipse GMF Runtime](https://projects.eclipse.org/projects/modeling.gmf-runtime) is an industry proven application framework for creating graphical editors using Eclipse EMF and Eclipse GEF.

The GMF Runtime provides many features that one would have to code by hand if using EMF and GMF directly.

* A set of reusable components for graphical editors, such as printing, image export, actions and toolbars and much more.
* A standardized model to describe diagram elements, which separates between the semantic (domain) and notation (diagram) elements.
* A command infrastructure that bridges the different command frameworks used by EMF and GEF.
* An extensible framework that allows graphical editors to be open and extendible.

### Source

* Clone: https://github.com/eclipse/gmf-runtime

### Issues

Issues are tracked in the Eclipse Bugzilla under the _GMF-Runtime_ product:

* [List of all open issues](https://bugs.eclipse.org/bugs/buglist.cgi?bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&classification=Modeling&component=General&list_id=20785712&product=GMF-Runtime&query_format=advanced)


### Building

From the top-level directory:

    mvn clean verify
    
You can build against a specific Target Platform using `-Dplatform=$PLATFORM_NAME`.
The supported platforms are available in the `org.eclipse.gmf.runtime.target` directory.
For example:

    mvn clean verify -Dplatform=2021-06

### CI

The official builds are executed on the Eclipse-provided Jenkins instance at <https://ci.eclipse.org/gmf-runtime/job/gmf-runtime-master/>

### Update Sites

Update Sites (p2 repositories) are available at:
* <https://download.eclipse.org/modeling/gmp/gmf-runtime/updates/interim>: nightly builds
* <https://download.eclipse.org/modeling/gmp/gmf-runtime/updates/milestones>: milestone builds
* <https://download.eclipse.org/modeling/gmp/gmf-runtime/updates/releases>: official releases

### License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
