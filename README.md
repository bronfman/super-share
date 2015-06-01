Super Share
===========

Super Share allows you to combine Google Drive and App Engine to create custom shares for your domain.

##Prerequisites

+   [Perform Google Apps Domain-Wide Delegation of Authority](https://developers.google.com/drive/web/delegation)
+   Create a project in the [Google Cloud Console](http://console.developers.google.com)

##Get the code

    mkdir super-share
    cd super-share
    git clone https://github.com/rlancer/super-share.git

## Add in an appengine-web.xml

```xml
<appengine-web-app xmlns="http://appengine.google.com/ns/1.0">
    <application>cloud-console-project-id</application>
    <version>1</version>
    <threadsafe>true</threadsafe>
    <sessions-enabled>false</sessions-enabled>
    <precompilation-enabled>true</precompilation-enabled>
    <system-properties>
        <property name="file.encoding" value="UTF-8"/>
        <property name="email" value="email_account_which_owns_the_folder@yourdomain.com"/>
        <property name="folder" value="id_of_folder_which_will_store_the_documents"/>
    </system-properties>
</appengine-web-app>
```

## Add your private key

The private key comes from performing domain wide delegation of authority.

Copy the .p12 file to *webapp/WEB-INF/privatekeys* directory


## Run the app locally

    > gradle appengineRun

A local server should be spawned, visit http://localhost:8080/file-name in your browser, if you see your file displayed then everything works!

## Deploy to App Engine

    > gradle appengineUpdate

## Set up your sub-domain

