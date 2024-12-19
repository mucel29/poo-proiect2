# Project Assignment POO - J. POO Morgan - Phase One

#### [Assignment Link](https://ocw.cs.pub.ro/courses/poo-ca-cd/teme/2024/proiect-e1)
<br>

#### Table of contents

<!-- TOC -->
* [Project structure](#project-structure)
* [Notable classes](#notable-classes)
  * [`Transaction`](#transaction)
  * [`Command`](#command)
* [Design](#design)
  * [Improvements from `Tema-0`](#improvements-from-tema-0)
  * [Patterns](#patterns)
* [Advanced concepts](#advanced-concepts)
  * [Nested classes](#nested-classes)
  * [Lambdas / Functional Interfaces](#lambdas--functional-interfaces)
  * [Records](#records)
  * [Streams](#streams)
  * [Reflection](#reflection)
    * [Why use reflection](#why-use-reflection)
    * [Reflection flow](#reflection-flow)
* [Program flow](#program-flow)
* [Feedback](#feedback)
<!-- TOC -->

## Project structure

* `src`
    * `checker` - Checker files
    * `fileio` - Contains classes used to read data from the json files _(deprecated)_
    * `io` - input / output utility classes
  
   * `system` - banking system package
     * `command` - system commands package
     * `exceptions` - custom exceptions used in this project
       * `handlers` - custom exception handlers
     * `exchange` - currency conversion package
     * `storage` - data storage package
     * `user` - user data structures package
     * `BankingSystem` - system manager singleton
     * `Transaction` - interface representing transactions

  * `util` - utilities package
    * `Graph` - Generic weighted graph implementation
    * `Pair` - Generic record for storing 2 objects
    * `Utils` - Utilities for generating IBANs and card numbers _(included regex iban verification method)_
    * `ReflectionUtils` - Common methods for using reflection _(see [Advanced Concepts](#reflection))_
* `input` - contains the tests in JSON format
* `ref` - contains all reference output for the tests in JSON format

## Notable classes

### `Transaction`
Defines the transaction interface, along with various nested class implementations:
* `Transaction.Base` - base class used by the system
* `Transaction.Transfer` - implementation representing a transfer between 2 accounts
* `Transaction.Payment` - implementation representing a card payment
* `Transaction.CardOperation` - implementation representing a card operation _(Creation / Destruction)_
* `Transaction.SplitPayment` - implementation representing a payment from multiple accounts

> * All transaction implementations are derived from `Transaction.Base` and only contain their specific fields.
> * All functionality is implemented by `Transaction.Base` and uses reflection to access the subclasses when exporting to `ObjectNode`
> * More details about the export process in [Advanced Concepts](#reflection)

### `Command`
Defines the command interface, along with an inner enum which stores all valid commands and supplier.

> `Command.Type(COMMAND_NAME, COMMAND_SUPPLIER)`
> * `COMMAND_NAME` - e.g. `"addAccount"`
> * `COMMAND_SUPPLIER` - e.g. `AddAccountCommand::fromNode`

All system commands define a static method `fromNode` for parsing the command details from a given `JsonNode`

## Design

### Improvements from `Tema-0`

* Removal of the `Constants` class by utilising enums for constant values
* `*Type` enums are now nested inside their corresponding classes instead of a separate file and are accessed through `*.Type` _(e.g. `Account.Type`)_
* Removal of `switch` statements in favor of `ENUM::fromString` for parsing and validation _(e.g. `Command.Type::parse` returns the `Command.Base` instance to be used)_
* Usage of exceptions for detecting erroneous requests in the system and handling them

### Patterns

* Singleton - `BankingSystem` uses the singleton pattern for quick access to the global state of the system
* Provider - `StorageProvider` and `ExchangeProvider` are used to allow easier extension of the system
> I've created two implementations for each provider to demonstrate the usage of the pattern:
> * `StorageProvider`
>   * `MemoryEfficientStorage` - stores only the users and makes use of the data hierarchy to retrieve `Account`s and `Card`s
>   * `MappedStorage` - maps every structure to a key for faster access but at the cost of memory
> * `ExchangeProvider`
>   * `BasicExchange` - only stores the given exchanges and their reversed counterparts
>   * `ComposedExchange` - can also compute indirect exchanges _(e.g. `RON` -> `USD` -> `EUR` -> `INR`)_

> I encourage you to change the `StorageProvider` used by modifying `BankingSystem::reset`

<br>

* The described providers also act as **DAO**s _(Data Access Object)_
> In software, a data access object (DAO) is a pattern that provides an abstract interface to some type of database or other persistence mechanism.
> <br>
> [Wikipedia page](https://en.wikipedia.org/wiki/Data_access_object)

## Advanced concepts

### Nested classes

This project makes use of nested classes to provide a cleaner file structure and better convey the usage scope of some classes

### Lambdas / Functional Interfaces

This project makes use of the `PathComposer` interface inside `Graph` to specify how a new node affects an existing path weight
<br>
Lambdas are used for simplifying the filtering / iterating code when working with [streams](#streams)

### Records
This project makes use of `record`s for simplifying `Pair<T>` and `Exchange`

### Streams

This project makes use of streams for:
* Elegant one-liners
* Quick filtering inside `MemoryEfficientStorage` and `SpendingReport`
* Mapping a list to another list _(e.g. `List<String>` to `List<Account>`)_

### Reflection

This project makes use of reflection for exporting `Transaction` instances.
<br>
<br>

#### Why use reflection
There are 3 reasons for using this approach in implementing transactions:
1. Simplifying the derived `Transaction`s code by including all the necessary code inside `Transaction.Base`
2. Removing duplicate field output code from the derived `Transaction`s
3. Flexing _(credit: Cleopatra)_ 
<br>

![puncte bonus meme](https://i.postimg.cc/s2q3NHVN/IMG-20241212-WA0012.png)

<br>

#### Reflection flow
* A `Transaction` instance calls `toNode` which calls the `Transaction.Base` `toNode` method
* `Transaction.Base` creates a new `ObjectNode`
* `Transaction.Base` writes its fields to the node by calling `ReflectionUtils::addField` on the fields using `this` as the `callerObject`
* `Transaction.Base` writes the fields of the runtime instance with the same call to `ReflectionUtils::addField` but iterating through `this.getClass().getDeclaredFields()`
* `ReflectionUtils::addField` determines the runtime type of the field and casts it's value accordingly 
* `root.put(field.getName(), (FIELD_TYPE) field.get(caller))`



## Program flow

* `BankingSystem` gets initialized
  * `User`s are registered in the `StorageProvider`
  * `Exchange`s are registered in `ExchangeProvider`
  * `Command`s are parsed
* `BankingSystem` starts running
  * Each `Command` executes
    * The command `Command` successfully executes
    * A `BankingException` occurs
      * It's handled by an `ExceptionHandler` inside the exception
      * The exception can't be handled, log the detailed error

<br>

> If you wish to see the full log messages, set `VERBOSE_LOGGING` variable inside `BankingSystem` to `true`

## Feedback

* A better organization would be preferred
* The OCW page should be in sync with the test files
* More examples should be provided on the OCW page
* More details should be provided about certain commands:
  * `splitPayment` should stop on the first account with insufficient balance or at least output all the accounts that fit this criteria
  * `printUsers` should treat both types of accounts
  * Erroneous queries should provide examples of transaction / command messages

