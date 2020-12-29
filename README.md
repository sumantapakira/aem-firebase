# aem-firebase
AEM integration with Firebase. 
Now you will be able to send push notification to end user smart phone from AEM. 

In this example, two use cases are shown, however you can implement the way you want using this example : 

a) Let the user know whenever there is a package installed in AEM
b) Notify user when someone sends workflow task in AEM

This OSGI module sends data to Firebase only to save recordes in real time database. After there is another application (https://github.com/sumantapakira/firebase-notification) which sends the notification.

You can download the JAR from here : https://github.com/sumantapakira/aem-firebase/blob/main/core/target/sumanta-android-aem-firebase.core-1.0-SNAPSHOT.jar

In order to build this, it is bit tricky as Firbase jar uses many jars internally. 

You have to add this this jars in your 1) .m2/repository/com/google (https://drive.google.com/file/d/1KWlv0hsXantfYRYFniuEhhRD4-lpdbWl/view?usp=sharing) and .m2/repository/io/grpc (https://github.com/sumantapakira/aem-firebase/blob/main/grpc.zip)


