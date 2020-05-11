# Cappybara

Proof-of-concept Android app to demonstrate a mobile framework for automatic and manual closed-captioning of user-generated video content. 

## Usage
Build and run Cappybara by opening this repository as a project in Android Studio.

This demonstration uses the Google Cloud Speech-to-Text client library, which requires a valid Google Cloud Platform service account. For the caption auto-generation portion of this app to work, you must have created a service account, a GCP project, a Google Cloud Storage bucket, and have downloaded a JSON copy of your service account key. Follow the instructions in the beginning of the `ASRGoogle.java` file to allow the app to access and use your credentials for speech to text recognition.