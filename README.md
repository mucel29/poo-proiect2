# Project Assignment POO - J. POO Morgan - Phase Two

#### [Assignment Link](https://ocw.cs.pub.ro/courses/poo-ca-cd/teme/2024/proiect-etapa2)
<br>

#### Table of contents

<!-- TOC -->
* [Project structure](#project-structure)
* [Notable classes](#notable-classes)
  * [`Transaction`](#transaction)
  * [`Command`](#command)
* [Design](#design)
  * [Improvements from `Phase One`](#improvements-from-phase-one)
  * [Patterns](#patterns)
* [Advanced concepts](#advanced-concepts)
  * [Nested classes](#nested-classes)
  * [Lambdas / Functional Interfaces](#lambdas--functional-interfaces)
  * [Records](#records)
  * [Streams](#streams)
  * [Reflection](#reflection)
    * [Why use reflection](#why-use-reflection)
    * [Reflection flow](#reflection-flow)
    * [The `getType` problem](#the-gettype-problem)
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
     * `commerce` - commerciants package
     * `exceptions` - custom exceptions used in this project
       * `handlers` - custom exception handlers
     * `exchange` - currency conversion package
     * `payments` - pending payments package
     * `storage` - data storage package
     * `user` - user data structures package
       * `plan` - service plans package
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
* `Transaction.PlanUpgrade`
* `Transaction.CashWithdrawal`
* `Transaction.InterestPayout`
* `Transaction.SavingsWithdrawal`

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

### Improvements from `Phase One`

* Usage of the `Amount` record for handling all money related actions
* Moving various checks and actions for `Account` into `Account::authorize*` methods

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

* Strategy - `CommerciantStrategy` uses this pattern to determine which cashback strategy to apply at runtime
> NOTE <br><br>
> The difference between `provider` and `strategy` is only conceptual, one using abstract classes and the other interfaces. This was discussed with Mr. Olteanu, and he advised me to mention the usage of these patterns.

<br>

* Factory - `StrategyFactory` & `ServicePlanFactory`
* Builder - `ServicePlan` through the `@Builder` annotation from lombok


* Observer - `PendingPayment` & `PaymentObserver`

> These interfaces are used to manage the new `SplitPayment` which waits for all observers (`User`) to accept the payment before notifying all of them to proceed with the payment, or to remove it in case of an error or rejection.

## Advanced concepts

### Nested classes

This project makes use of nested classes to provide a cleaner file structure and better convey the usage scope of some classes

### Lambdas / Functional Interfaces

This project makes use of the `PathComposer` interface inside `Graph` to specify how a new node affects an existing path weight

Lambdas are used for simplifying the filtering / iterating code when working with [streams](#streams)

### Records
This project makes use of `record`:
* `Pair<T>` - for grouping two unrelated objects
* `Exchange` - for grouping two currencies and their exchange rate
* `Amount` - for keeping track of funds & cleaner conversions between currencies
* `PaymentOrder` - for grouping all the details to be sent to an observer regarding a pending payment
* `BusinessAccount.AssociateData` - for keeping track of associates

### Streams

This project makes use of streams for:
* Elegant one-liners
* Quick filtering inside `MemoryEfficientStorage` and `SpendingReport`
* Mapping a list to another list _(e.g. `List<String>` to `List<Account>`)_

### Reflection

This project makes use of reflection for exporting `Transaction` instances.

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

<br>

#### The `getType` problem

After a quick read into the grading guidelines available on the OCW page, in the section about `OOP penalties` there was a penalty of `1/10` for using `instanceof` or `getClass`.

After asking Cleopatra if this also applies to `getType`, which is heavily used inside `ReflectionUtils`, she confirmed that it also leads to a penalty.

Now I'm not arguing that `getType`, even reflection in general, breaks OOP principles. I totally agree there, I would try to avoid reflection at all costs in a real project.

What I'm trying to show is an advanced understanding of Java features and concepts, including reflection.

While supporting the above statement about OOP principles, it needs to be noted that `Jackson` also makes use of reflection for its serialization and deserialization processes _(unless the fields are annotated)_.

Considering the above statement, I think my usage of `getType` should not warrant a penalty as it's being used only during the writes to the `StateMapper` and not throughout the whole project.
> _I think this usage compensates for the manual parsing done by `IOUtils` instead of using the `fileio` package_

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

> Please keep in mind that this is not an attack to any particular person. These are just my thoughts about the project.

<br>

* A better organization would be preferred
* The OCW page should be in sync with the test files _(at least it's better than phase one)_
* The more advanced features should be explained plainly, without a wall of text on the forum serving as an example.
_(If it takes that much effort to explain a feature correctly, then it might be better to rethink it)_
* The countless revisions of the test files should be noted here.

<br>

> This whole project phase felt like a big process of trial and error. I spent countless hours debugging features that at a first glance should be logical, but implemented poorly.
> 
> A good example would be the deposit and spending limits on the `BusinessAccount`.
> 
>While a normal person might think that a spending limit is applied for the cumulated transactions made by an associate, the reference implementation uses the current transaction amount for checking whether the limit was reached or not.