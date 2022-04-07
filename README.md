# Android OAuth with Biometrics

🔐 A proof-of-concept application to log into an example Auth0 instance with a PKCE flow and allow secure retrieval of the access and refresh tokens with biometrics

<img src=".docs/demo.gif" alt="Login, reauthentication with biometrics, and logout flows" width="400" />

**Features:**

- Login + logout flows with an OAuth provider
- Store credentials in secure storage with hardware-backed cryptography
- Graceful handling of devices that lack hardware-backed cryptography features
- Intelligent selection of the best available cryptography + biometrics for enhanced security based on API level and available hardware
- Reauthenicate back into an account with biometrics
- Graceful handling of devices which have biometric capabilities, but no enrolled biometrics
- Network stack automatically injects the acccess token into the necessary API calls
- Auto-refresh of the access token
- Auto-logout when the refresh token is invalidated
- Identification of what kind of secondary authentication mechanisms are available on the device
- Showing what kind of secondary authentication was used

**Here are some useful sources that I consulted before building this project:**

- [Using BiometricPrompt with CryptoObject](https://medium.com/androiddevelopers/using-biometricprompt-with-cryptoobject-how-and-why-aace500ccdb7)
- [Biometric Authentication on Android - Part 1](https://medium.com/androiddevelopers/biometric-authentication-on-android-part-1-264523bce85d)
- [Biometric Authentication on Android - Part 2](https://medium.com/androiddevelopers/biometric-authentication-on-android-part-2-bc4d0dae9863)
- [OAuth2 + PKCS + Auth0](https://medium.com/geekculture/implement-oauth2-pkce-in-swift-9bdb58873957)
- [AppAuth for Android](https://github.com/openid/AppAuth-Android)

## Setup an Auth0 Account

Since this project requires an OAuth IDP to run, follow these steps:

1. Create a free [Auth0 account](https://auth0.com/)
1. Once you have created an account, create a tenant (which can be thought of as a new project)
1. Inside of the newly created tenant, create a new application by going to the navigation panel &gt; Applications &gt; Create Application &gt; Native
1. Open up your new application and make a note of the Client ID and the Domain under the Settings tab
1. On the same tab, add `com.oliverspryn.android.oauthbiometrics://oauth/login` to the Allowed Callback URLs list
1. Add `com.oliverspryn.android.oauthbiometrics://oauth/logout` to the Allowed Logout URLs list
1. Create a user for your testing purposes by going to the navigation panel &gt; User Management &gt; Users &gt; Create User

## Run the Project

Once the OAuth tenant, application, and user account are setup, you can incorporate them into this project:

1. Clone the project
1. Open up `app/build.gradle`
1. Change the `OAUTH_CLIENT_ID` and `OPENID_CONFIG_URL` to the values you found in the Auth0 management portal
1. Configure `CryptographyConfig.ALLOW_DEVICE_CREDENTIALS_AS_SECONDARY_LOGIN` to allow or disallow PIN/pattern/etc as an additional secondary login option
