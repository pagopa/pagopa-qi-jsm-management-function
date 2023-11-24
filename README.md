# pagoPA Functions template

Java template to create an Azure Function.

## Function examples

There is an example of a Http Trigger function.

---

## Run locally with Docker

`docker build -t pagopa-functions-template .`

`docker run -p 8999:80 pagopa-functions-template`

### Test

`curl http://localhost:8999/example`

## Run locally with Maven

`mvn clean package`

`mvn azure-functions:run`

### Test

`curl http://localhost:7071/example`

---

## TODO

Once cloned the repo, you should:

- to deploy on standard Azure service:
    - rename `deploy-pipelines-standard.yml` to `deploy-pipelines.yml`
    - remove `helm` folder
- to deploy on Kubernetes:
    - rename `deploy-pipelines-aks.yml` to `deploy-pipelines.yml`
    - customize `helm` configuration
- configure the following GitHub action in `.github` folder:
    - `deploy.yml`
    - `sonar_analysis.yml`

Configure the SonarCloud project :
point_right: [guide](https://pagopa.atlassian.net/wiki/spaces/DEVOPS/pages/147193860/SonarCloud+experimental).

## JIRA integration

To open an issue on Jira Service Management service the following parameters should be configured

| parameter     | usage          | description                            |
|---------------|----------------|----------------------------------------| 
| username      | rest client    | username used to authenticate on JSM   |
| password      | rest client    | password used to authenticate on JSM   |
| project id    | issue creation | project key where issue will be opened |
| issue type id | issue creation | id of the issue to be opened           |

Project id uniquely identifies the JSM project.
To retrieve issue type id for open an issue follow the below steps:

First of all you have to be logged in with your Jira account.

Once connected, in order to create an issue you have to retrieve request types for your project

Retrieve request types (replacing `_projectId_` with your JSM project identifier):

```
GET https://pagopa.atlassian.net/rest/servicedeskapi/servicedesk/_projectId_/requesttype
```

With the above api call you can retrieve the requestTypeId for your project to set this value into configuration.

Fields details can be explored with this call(replacing `_projectId_` with your JSM project identifier
and `_requestFieldId_` with the request `issueTypeId retrieved by the above api call response):

```
GET https://pagopa.atlassian.net/rest/servicedeskapi/servicedesk/_projectId_/requesttype/_requestFieldId_/field
```