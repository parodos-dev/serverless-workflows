# DefaultApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createTask**](DefaultApi.md#createTask) | **POST** /api/scaffolder/v2/tasks | Create a New Task |
| [**deleteTask**](DefaultApi.md#deleteTask) | **DELETE** /api/scaffolder/v2/tasks/{taskId} | Delete Task |
| [**getTaskDetails**](DefaultApi.md#getTaskDetails) | **GET** /api/scaffolder/v2/tasks/{taskId} | Get Task Details |
| [**getTemplateParameterSchema**](DefaultApi.md#getTemplateParameterSchema) | **GET** /api/scaffolder/v2/templates/{namespace}/{kind}/{name}/parameter-schema | Get Template Parameter Schema |
| [**listAvailableActions**](DefaultApi.md#listAvailableActions) | **GET** /api/scaffolder/v2/actions | List Available Actions |
| [**listTasks**](DefaultApi.md#listTasks) | **GET** /api/scaffolder/v2/tasks | List Tasks |
| [**updateTask**](DefaultApi.md#updateTask) | **PATCH** /api/scaffolder/v2/tasks/{taskId} | Update Task |


<a name="createTask"></a>
# **createTask**
> createTask_201_response createTask(Task)

Create a New Task

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **Task** | [**Task**](../Models/Task.md)| Task creation parameters | |

### Return type

[**createTask_201_response**](../Models/createTask_201_response.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="deleteTask"></a>
# **deleteTask**
> deleteTask(taskId)

Delete Task

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **taskId** | **String**|  | [default to null] |

### Return type

null (empty response body)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

<a name="getTaskDetails"></a>
# **getTaskDetails**
> Task getTaskDetails(taskId)

Get Task Details

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **taskId** | **String**|  | [default to null] |

### Return type

[**Task**](../Models/Task.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getTemplateParameterSchema"></a>
# **getTemplateParameterSchema**
> Template getTemplateParameterSchema(namespace, kind, name)

Get Template Parameter Schema

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **namespace** | **String**|  | [default to null] |
| **kind** | **String**|  | [default to null] |
| **name** | **String**|  | [default to null] |

### Return type

[**Template**](../Models/Template.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listAvailableActions"></a>
# **listAvailableActions**
> List listAvailableActions()

List Available Actions

### Parameters
This endpoint does not need any parameter.

### Return type

[**List**](../Models/listAvailableActions_200_response_inner.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="listTasks"></a>
# **listTasks**
> List listTasks(createdBy, status)

List Tasks

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createdBy** | **String**|  | [optional] [default to null] |
| **status** | **String**|  | [optional] [default to null] |

### Return type

[**List**](../Models/Task.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="updateTask"></a>
# **updateTask**
> updateTask(taskId, Task)

Update Task

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **taskId** | **String**|  | [default to null] |
| **Task** | [**Task**](../Models/Task.md)| Task update parameters | |

### Return type

null (empty response body)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined

