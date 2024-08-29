# Documentation for Notifications Plugin - OpenAPI Specs

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *NotificationsApi* | [**createNotification**](Apis/NotificationsApi.md#createnotification) | **POST** /api/notifications | Create a notification |
*NotificationsApi* | [**getNotifications**](Apis/NotificationsApi.md#getnotifications) | **GET** /api/notifications | Get user's notifications |
*NotificationsApi* | [**getStatus**](Apis/NotificationsApi.md#getstatus) | **GET** /api/notifications/status | Get user's count of unread+read notifications |
*NotificationsApi* | [**setRead**](Apis/NotificationsApi.md#setread) | **POST** /api/notifications/update | Set read/unread notifications |


<a name="documentation-for-models"></a>
## Documentation for Models

 - [Count](./Models/Count.md)
 - [CreateBody](./Models/CreateBody.md)
 - [CreateBody_recipients](./Models/CreateBody_recipients.md)
 - [GetNotifications](./Models/GetNotifications.md)
 - [Notification](./Models/Notification.md)
 - [NotificationPayload](./Models/NotificationPayload.md)
 - [SetReadRequest](./Models/SetReadRequest.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="BearerToken"></a>
### BearerToken

- **Type**: HTTP Bearer Token authentication

