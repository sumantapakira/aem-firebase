# aem firebase
AEM integration with Firebase. 
Now you will be able to send push notification to end user smart phone from AEM. 

In this example, two use cases are shown, however you can implement the way you want using this example : 

a) Let the user know whenever there is a package installed in AEM
b) Notify user when someone sends workflow task in AEM

Demo : https://youtu.be/BOHmYTW4Uzc

This OSGI module sends data to Firebase only to save recordes in real time database. After there is another application (https://github.com/sumantapakira/firebase-notification) which sends the notification.

You can download the JAR from here : https://drive.google.com/file/d/1nI0dqe8CJBWkv1sig8Xzbi-ABrdeWvjP/view?usp=sharing

In order to build this, it is bit tricky as Firbase jar uses many jars internally. 

You have to add this this jars in your 1) .m2/repository/com/google (https://drive.google.com/file/d/1KWlv0hsXantfYRYFniuEhhRD4-lpdbWl/view?usp=sharing) and .m2/repository/io/grpc (https://github.com/sumantapakira/aem-firebase/blob/main/grpc.zip)

# Follow this steps on how to configure

1. You also need to add service user "firebaseauth" and add sling service user mapping as follows : sumanta.android.aem.firebase.sumanta-android-aem-firebase.core=firebaseauth

2. Add this into your sling.properties file : sling.bootdelegation.bouncycastle=org.bouncycastle.*

3. Whitelist the bundle in Felix console 

