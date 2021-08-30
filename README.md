# Blackjack-Helper

## Description

Decision support tool that answer: ***How much to bet?*** and ***Which action to take?*** at every step of the way for the game of Blackjack.  

## Install

#### Build from source

```shell
mvn clean package
java -jar target/Blackjack-Helper-1.0-SNAPSHOT.jar
```

#### Download executable from [release](https://github.com/VincentPinet/Blackjack-Helper/releases)

[direct link](https://github.com/VincentPinet/Blackjack-Helper/releases/v1.0/download/Blackjack-Helper-1.0.jar)

## Demo

![](/assets/demo.png)

0. Configurable rule variants (see [Rule Variants](#rule-variants))
1. Dealer's upcard
2. Discard tray
3. Player's hand
4. Splitting hand
5. Shoe composition (drag and drop to draw from)
6. EV of the shoe & Kelly criterion f* (see [Bankroll management](#bankroll-management))
7. Excepted value in bet unit for each action
8. Shuffle the shoe
9. Play next hand

## Rule Variants

|  Adjustable                           | Fixed                                         |
| ------------------------------------- | ----------------------------------------------|
| S-17<br/> *Stand on soft 17*          | ENHC <br/>*Dealer does not peek at hole card* |
| DAS<br/> *Double after split*         | NRS <br/>*No resplit*                         |
| DOA<br/> *Double on any 2 (as opposed to [9-11] only)* |   |
| HSA<br/> *Hit split aces*             |  |
| ES10<br/> *Surrender against non ace* |  |
| XD<br/> *Number of decks in the shoe* |  |
| BJ pays<br/> *Blackjack payout*       |  |

## Method

Purely a combinatorial analysis (fully composition dependent), meaning :  
- no predefined strategy (i.e. basic chart)  
- no abstraction of hands (i.e. hard14, soft15, …)  
- no abstraction of shoe (i.e. counting)  
- no abstraction of bets (i.e. bet spread)  
- no simulation  

#### About splitting

When computing the expected value of a shoe, the expected value of a split hand is computed without knowledge of cards in the other one. Meaning, splitting a pair of 8s for instance would be equal to twice the excepted value of starting a hand with an 8 on a shoe with an extra 8 missing.  
Instead of considering every pair of two cards from the shoe, or even more, considering playing the second hand differently depending on what the first one ended up having.  
One goal of this software is being able to run concurrently with and by someone actually playing, so we are constraint by our decisions time and it seems to be a fair compromise to make given how computationally intensive it becomes for marginal precision improvement.  
*Note : When navigating a specific instance of split hands in the UI both hands will have complete information, this is just not done pre split in the computation of the overall ev of the whole shoe.*  

## Bankroll management

Once we established the optimal strategy (according the maximizing ev metric). We can optimize our excepted growth rate by looking at the resulting outcome distribution of this strategy and apply the generalized Kelly criterion.  
For instance in the rule and shoe configuration of the demo screenshot, you would end up with this :

| ![](https://latex.codecogs.com/svg.latex?b_i) | ![](https://latex.codecogs.com/svg.latex?p_i) |
| ---- | ------ |
| -4   | 0.01%  |
| -3   | 0.07%  |
| -2   | 3.05%  |
| -1   | 36.33% |
| -0.5 | 9.91%  |
| 0    | 8.47%  |
| 1    | 31.35% |
| 1.5  | 5.43%  |
| 2    | 5.23%  |
| 3    | 0.11%  |
| 4    | 0.03%  |

We now find ![](https://latex.codecogs.com/svg.latex?x) *(faction of bankroll to wager)* that maximized the expected growth rate defined as :  
![](https://latex.codecogs.com/svg.latex?f(x)=\sum_{i}(p_i*log(1+b_i*x)))  

#### Side note

What if there is a strategy that is not entirely based on maximizing ev, but takes into account lowering the volatility to some degree in the hope that it will allow bigger bet size that improve the growth rate.  
This is theoretically possible but chances are, we won't be able to raise the bet size in a significant enough way to recoup the immediate ev loss of a worst strategy. Interesting idea nonetheless, worth researching a bit.

## Disclaimer

Don't gamble without an edge, duh.  
