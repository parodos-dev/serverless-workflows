# Task
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **id** | **String** | A unique identifier for the task. | [optional] [default to null] |
| **status** | **String** | The current status of the task (e.g., pending, in progress, completed). | [optional] [default to null] |
| **createdBy** | **String** | The identifier of the user or system that created the task. | [optional] [default to null] |
| **templateRef** | **String** | A reference to the template used to create the task. | [optional] [default to null] |
| **values** | [**Object**](.md) | The specific values provided when the task was created, based on the template&#39;s schema. | [optional] [default to null] |
| **secrets** | [**Object**](.md) | Sensitive data associated with the task, such as API keys or credentials, handled securely. | [optional] [default to null] |
| **logs** | **List** | A collection of log entries related to the task&#39;s execution, useful for debugging or auditing. | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

