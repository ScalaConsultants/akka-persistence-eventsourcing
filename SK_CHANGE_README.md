Description of refactoring by separating actors from domain.
===

## Possible improvements in artifacts location
Both actors.scala files and AggregateRootActor and AggregateManager should be moved outside the domain to some
packages (e.g. actors -> "infrastructure", ARA and AM -> "aggregate.actors"). But I'm not so deep in this project to
find define better places.

## Maybe we could create library?
AggregateRootActor and AggregateManager have now strictly defined responsibility and interfaces so it is possible to
make some library from them.

## Cause of naming convention inconsistency
Naming convention connected with AggregateRoot are different than those connected with AggregateManager (CommandAdapter)
although refactoring looks very similar for both. In AggregateRoot we have UserAggregate and UserAggregateActor but in
AggregateManager there is no info in name that AggregateManager is an actor and pair for actor UserAggregateManager is
a domain: UserCommandAdapter. I was considering making CommandAdapter like things consistent with naming from
AggregateRoot but CommandAdapter does not have an interface which can be called manager :( This can mean that maybe this
part of refactor is less natural and should be done other way around.

## Biggest lacks
- This refactoring will be enough for domain until it will be no call within aggregate root ro some service. In such
case actor interface will have to be used which can be changed by adding some domain interfaces which should be used
externally and by domain artifacts and should be implemented using actors (probably by delegation to `!`). After
introducing this change it'll be possible to really well hide actors presence in system. (And don't get me wrong I have
nothing against Akka but possibility to replace any infrastructure is usually very good when you have to
migrate "from" or "to" and when you are testing things)
- I didn't write a unit tests for domain classes but it should be quite obvious that it is now possible to do it without
any actors artifacts :) .

## Other notes
- What if someone is publishing few events? We will not know which should be used to change state, but this is not worse
now than it was.
- What if one incoming command is changed into few commands? Again this problem is valid for version without refactoring also.

