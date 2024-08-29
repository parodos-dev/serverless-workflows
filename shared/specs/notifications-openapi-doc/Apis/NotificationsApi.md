# NotificationsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createNotification**](NotificationsApi.md#createNotification) | **POST** /api/notifications | Create a notification |
| [**getNotifications**](NotificationsApi.md#getNotifications) | **GET** /api/notifications | Get user&#39;s notifications |
| [**getStatus**](NotificationsApi.md#getStatus) | **GET** /api/notifications/status | Get user&#39;s count of unread+read notifications |
| [**setRead**](NotificationsApi.md#setRead) | **POST** /api/notifications/update | Set read/unread notifications |


<a name="createNotification"></a>
# **createNotification**
> List createNotification(CreateBody)

Create a notification

    Create a notification

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **CreateBody** | [**CreateBody**](../Models/CreateBody.md)|  | [optional] |

### Return type

[**List**](../Models/Notification.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="getNotifications"></a>
# **getNotifications**
> GetNotifications getNotifications()

Get user&#39;s notifications

    Get user&#39;s notifications

### Parameters
This endpoint does not need any parameter.

### Return type

[**GetNotifications**](../Models/GetNotifications.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="getStatus"></a>
# **getStatus**
> Count getStatus()

Get user&#39;s count of unread+read notifications

    Get user&#39;s count of unread+read notifications

### Parameters
This endpoint does not need any parameter.

### Return type

[**Count**](../Models/Count.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="setRead"></a>
# **setRead**
> List setRead(SetReadRequest)

Set read/unread notifications

    Set read/unread notifications

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **SetReadRequest** | [**SetReadRequest**](../Models/SetReadRequest.md)|  | [optional] |

### Return type

[**List**](../Models/Notification.md)

### Authorization

[BearerToken](../README.md#BearerToken)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

