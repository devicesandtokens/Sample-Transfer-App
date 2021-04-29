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
