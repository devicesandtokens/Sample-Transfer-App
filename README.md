# Sample-Transfer-App
Let's discover **Smart POS in less than 5 minutes**.

## Getting Started

SmartPOS SDK is a simple to use SDK for the purpose of building Android App on POS terminals
The SDK is divided into three(3) modules namely -

[`smart-pos-core`](#)
[`smart-pos-emv-pax`](#)
[`smart-pos-usb`](#)

Coupled with this libraries, you will need to also add a network wrapper class to help with converter class for xml responses.
[`Ebi Simple adaptar`](https://github.com/ebi-igweze/simple-call-adapter)

this three modules should be downloaded and added to your project.
In addition to these modules, you need to use eze simple library to so that the app will work  better.


### To start using the SDK, follow the steps below:

add module smart-pos-core and module smart-pos-emv-pax to your project. If you want to make use of USB communication between the SDK and a PC, add the third module smart-pos-usb
Configure the SDK in your android Application class or your main Activity or Fragment or Application class like below


```jsx
// create POSDevice Implementation instance for pax device

POSDeviceImpl device  = POSDeviceImpl.create(getApplicationContext());

String clientId = "your-client-id";
String clientSecret = "your-client-secret";
String alias = "your-alias";
String merchantCode = "your-merchant-code";

// initialize POSConfig
POSConfig config = new POSConfig(alias, clientId, clientSecret, merchantCode);

// setup terminal
IswPos.setupTerminal(getApplication(), device, config);
```

Configure Terminal
In order to communicate effectively with the terminal, some initialization and configuration needs to be done.

### GET TERMINAL CONFIG
The first thing to do is to get the terminal configuration of the pos from the server. After this you need to save the terminal configuration. Then you download your keys.
``` jsx
// get terminal info
IswHandler().downloadTmKimParam()

// download keys
IswHandler().downloadKeys(terminalId: String, ip: String, port: Int, isNibbsTest: Boolean?)

// Save the terminal terminal info
IswHandler().saveTerminalInfo(terminalInfo: TerminalInfo)

// retrieve the terminal info
IswHandler().getTerminalInfo()

```

### PROCESS TRANSACTION
For you to process a successful transaction you need to first SetupTransaction, Start transaction, and then send your transaction to the server.

The code below explains the steps to take.

// start transaction this should be called after [`setupTransaction method`](#)

```jsx
// setup transaction
// this channel is a coroutine channel that you use in broadcasting messages for the transaction

IswHandler().setupTransaction(amount: Int, terminalInfo: TerminalInfo, scope: CoroutineScope, channel: Channel<EmvMessage>)

 /**
     * this method should be called when the card is inserted or when you want to start reading the card
     * after [setupTransaction] method has returned a card read message
     * @return[EmvResult]**/

IswHandler().startTransaction()


// if you want to perform a transfer transaction, use this method
/**
      *This function is used in processing transfer transaction,
      * @param [terminalInfo]
      * @param [paymentModel]
      * @param [accountType]
      * @param [destinationAccountNumber]
      * @param [receivingInstitutionId]
      * @return [CardReadTransactionResponse]*/

IswHandler().processTransferTransaction(
             paymentModel: PaymentModel,
             accountType: AccountType,
             terminalInfo: TerminalInfo,
             destinationAccountNumber: String? = "",
             receivingInstitutionId: String? = "")

```

### Print Transaction Receipt
After a complete transaction you will get a response from the as the transaction result `CardTransactionResponse` class which contains information about the transaction result and some other informations.
To print the receipt of the transaction, you will have to create a layout in any way you like and then convert the page to a bitmap. This bitmap is what you will send to the printer.

```jsx
// this returns a PrintStatus class as a response
IswHandler(posDevice).printslip(bitmap)

```

To avoid uneccesary error during printing, you need to check the printer status.

```jsx
// this returns a PrintStatus class as a response
IswHandler(posDevice).checkPrintStatus()

```

Smart POS is built to support different programming patterns.
You can choose to you use MVVM, MVC and any other pattern.


### Note

ADB must be installed and added to Path enviroment variable on the PC
USB Debugging must be enabled in the Android terminal

### Please add the following dependencies to your app module gradle file to avoid resouurce not found error and other gradle errors
```jsx

// core dependencies
        koin_version = '1.0.1'
        retrofit_version = '2.5.0'
        zxing_version = '3.3.0'
        coroutine_version = '1.3.1'
        arch_lifecycle_version = '1.1.1'
        arch_paging_version = '1.0.1'
        koptional_version = '1.6.0'
        send_grid_version = '4.4.1'
        multidex_version = '2.0.1'

        // test dependencies
        mockito_version = '2.0.0'
        okhttp_version = '3.12.1'
        robolectric_version = '4.0.2'
        androidx_version = '1.0.0'
        test_runner_version = '1.0.2'
        concurrent_version = '0.4.4'



   implementation "org.koin:koin-core:$koin_version"
    implementation "org.koin:koin-android-viewmodel:$koin_version"

    implementation 'com.tapadoo.android:alerter:3.0.0'
    implementation "com.google.zxing:core:$zxing_version"
    implementation "com.igweze.ebi:simple-call-adapter:1.0.1"

    // kotlin optional lib
    implementation "com.gojuno.koptional:koptional:$koptional_version"

    // retrofit http library
    implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
    implementation "com.squareup.retrofit2:converter-scalars:$retrofit_version"

    implementation 'com.squareup.okhttp3:logging-interceptor:3.8.0'
    implementation "com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+"
    // retrofit gson converter
    implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
//    // xml converter
//    implementation ('com.thoughtworks.xstream:xstream:1.4.11') {
//        exclude group: 'xmlpull', module: 'xmlpull'
//        exclude group: 'org.xmlpull.v1.XmlPullParserException'
//    }
    // picasso image library
    implementation('com.squareup.picasso:picasso:2.71828') {
        exclude group: "com.android.support"
    }


    // kotlin coroutine libs
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutine_version"

    // architecture lifecycle components
    implementation 'androidx.lifecycle:lifecycle-runtime:2.1.0'
    implementation 'androidx.lifecycle:lifecycle-extensions:2.1.0'

    // architecture paging
    implementation 'androidx.paging:paging-runtime:2.1.0'

    // realm monarch pagging library
    implementation 'com.github.Zhuinden:realm-monarchy:0.5.1'

    implementation("com.journeyapps:zxing-android-embedded:$zxing_version") {
        exclude group: "com.android.support"
    }

    // retrofit xml converter
    implementation('com.squareup.retrofit2:converter-simplexml:2.0.0-beta3') {
        exclude group: 'stax', module: 'stax-api'
        exclude group: 'stax', module: 'stax'
    }

    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'com.android.support:appcompat-v7:28.0.0'
    implementation 'com.android.support.constraint:constraint-layout:1.1.3'
    implementation 'com.android.support:support-v4:28.0.0'
    testImplementation 'junit:junit:4.12'
    testImplementation "com.nhaarman.mockitokotlin2:mockito-kotlin:$mockito_version"
    testImplementation "org.robolectric:robolectric:4.0.2"
    testImplementation "org.koin:koin-test:$koin_version"
    testImplementation "androidx.test:core:$androidx_version"
    testImplementation "com.squareup.okhttp3:mockwebserver:$okhttp_version"
    testImplementation "net.jodah:concurrentunit:$concurrent_version"
    androidTestImplementation 'androidx.test:runner:1.2.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'
    androidTestImplementation 'androidx.test:rules:1.2.0'
    androidTestUtil 'androidx.test:orchestrator:1.2.0'

    kaptAndroidTest "androidx.databinding:databinding-compiler:3.5.0"

    implementation 'com.pixplicity.easyprefs:library:1.9.0'
    implementation 'com.airbnb.android:lottie:3.6.1'
    implementation 'com.github.javadev:underscore:1.64'
```
