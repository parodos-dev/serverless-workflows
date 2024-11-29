# Documentation for Scaffolder API

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *DefaultApi* | [**createTask**](Apis/DefaultApi.md#createtask) | **POST** /api/scaffolder/v2/tasks | Create a New Task |
*DefaultApi* | [**deleteTask**](Apis/DefaultApi.md#deletetask) | **DELETE** /api/scaffolder/v2/tasks/{taskId} | Delete Task |
*DefaultApi* | [**getTaskDetails**](Apis/DefaultApi.md#gettaskdetails) | **GET** /api/scaffolder/v2/tasks/{taskId} | Get Task Details |
*DefaultApi* | [**getTemplateParameterSchema**](Apis/DefaultApi.md#gettemplateparameterschema) | **GET** /api/scaffolder/v2/templates/{namespace}/{kind}/{name}/parameter-schema | Get Template Parameter Schema |
*DefaultApi* | [**listAvailableActions**](Apis/DefaultApi.md#listavailableactions) | **GET** /api/scaffolder/v2/actions | List Available Actions |
*DefaultApi* | [**listTasks**](Apis/DefaultApi.md#listtasks) | **GET** /api/scaffolder/v2/tasks | List Tasks |
*DefaultApi* | [**updateTask**](Apis/DefaultApi.md#updatetask) | **PATCH** /api/scaffolder/v2/tasks/{taskId} | Update Task |


<a name="documentation-for-models"></a>
## Documentation for Models

 - [Task](./Models/Task.md)
 - [Template](./Models/Template.md)
 - [Template_steps_inner](./Models/Template_steps_inner.md)
 - [createTask_201_response](./Models/createTask_201_response.md)
 - [listAvailableActions_200_response_inner](./Models/listAvailableActions_200_response_inner.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="BearerToken"></a>
### BearerToken

- **Type**: HTTP Bearer Token authentication

