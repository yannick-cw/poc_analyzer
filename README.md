# The Political Opinion Classifier

This is a project by Yannick Gladow and Simon Wanner.
The goal is to correctly classify political opinion based on data from reddit.

## The Idea
What if you could predict the political opinion of a person automatically by just looking at the posts he does in social media?   
This was the question, which lead to our idea, to classify text to different political groups.  
We considered doing it for the German political spectrum but than decided to try to differentiate between republicans and democrats, for the simple reason, that there are huge amounts of data available for US politics.  
As source for data we considered using twitter, but decided to go for [reddit](http://reddit.com), because it is easier to find labeled training data there.
## The Data
Reddit is a huge datasource for labeled data, there are millions of posts to almost every existing topic. 
And these posts are upvoted or downvoted by other users of the site. 
Thereby labeled data, rated by acceptance, is created by the community.  
Although maybe not 100% accurate, we considered upvoted posts in subreddits of one political opinion to correspond to that political opinion.  
For our project we extracted posts from the following subreddits.
##### Democratic Subreddits
- [/democrats](http://reddit.com/r/democrats)
- [/liberal](http://reddit.com/r/liberal)
- [/SandersForPresident](http://reddit.com/r/SandersForPresident)
- [/hillaryclinton](http://reddit.com/r/hillaryclinton)
- [/obama](http://reddit.com/r/obama)

##### Republican Subreddits
- [/republican](http://reddit.com/r/republican)
- [/republicans](http://reddit.com/r/republicans)
- [/conservative](http://reddit.com/r/conservative)
- [/AskTrumpSupporters](http://reddit.com/r/AskTrumpSupporters)
- [/The_Donald](http://reddit.com/r/The_Donald)

From these two groups we gathered over 7Gbyte of raw json files containing roughly 4.000.000 distinct text documents, labeled with upvotes.

## The Software Architecture
We wanted to implement this project in a highly modular microservice fashion.  
As language we decided to go with [scala](http://www.scala-lang.org/), because it for us it seemed to be the optimal fit for working with BigData and Machine Learning. 

First we extracted the json data from reddit with the help of a python [script](https://github.com/peoplma/subredditarchive).  
After that the data is processed through multiple microservices, each communicating via a REST api.
  
#### [The Importer](https://github.com/yannick-cw/poc-importer)  
The importer reads in the json files from a directory and then processes them in a streaming fashion.  First it is desirialized into an internal object representation, than it is sanitized, grouped into bulks and finally stored to an [elasticsearch](https://www.elastic.co/products/elasticsearch) database.

#### [The Cleaner](https://github.com/yannick-cw/poc_cleaner)  
The cleaner accepts http requests containing a text and gives back a text without stopwords and all words are stemmed with an implementation of the [Porter-Stemmer-Algorithm](https://de.wikipedia.org/wiki/Porter-Stemmer-Algorithmus).   
Furthermore relics from URL encoding are removed.
#### [The Analyzer](https://github.com/yannick-cw/poc_analyzer)
The Analyzer is the heart of the whole project.   
Firstly it reads in the labeled texts from the [elasticsearch](https://www.elastic.co/products/elasticsearch) database.  
After that multiple models are build with the classification algorithms that we chose.  
When the models are ready, it accepts http requests containing an algorithm name and a text and tries to classify this text. Therefore the input text is first cleaned up by the Cleaner mircorservice and then classified by the selected algorithm.  

The seconds mode of operation is the validation phase. There each model is build and tested against a specified percentage of the input data. The input data is randomly distributed into test and train data.
#### [The Frontend](https://github.com/yannick-cw/poc_frontend)  
The Frontend is build with the [play frameworke](https://www.playframework.com/) and his main job is to send the user requests to the backend and display the results.

#### More Technologies used
To make it easy to deploy this everywhere, each service can be run in a [docker](https://www.docker.com/) container.  
The whole stack is running on an [AWS](https://aws.amazon.com) instance with 16Gbyte of RAM and 8 cores.  
For classification the machine learning library [weka](http://www.cs.waikato.ac.nz/ml/weka/) is used.

![structure](https://github.com/yannick-cw/poc_analyzer/blob/master/pocStructure.png "Architecture")


## The Algorithms considered
#### The Featurevector
We tried different approaches to build a feature vector from the text to classify.   
One approach was to build a feature vector containing of:
- average spelling errors per word
- average text length
- average sentence length
- average distinct words
- average uppercase letters used
- average word length

From this approach we learned, that it does not work at all. All these features, combined or individually, where distributed very event between the political parties.  

So we decided to classify with the [bag of words approach](https://en.wikipedia.org/wiki/Bag-of-words_model).  
Furthermore we also tried to use n-grams but found no improvement in the classification results.

We considered the following algorithms, because they all seemed to create good results for text classification.

- [naive bayes](https://en.wikipedia.org/wiki/Naive_Bayes_classifier)
- [bayes net](https://en.wikipedia.org/wiki/Bayesian_network)
- [mulinomial naive bayes](https://en.wikipedia.org/wiki/Naive_Bayes_classifier#Multinomial_naive_Bayes)
- [support vector machine](https://en.wikipedia.org/wiki/Support_vector_machine)
- [j48 tree](https://en.wikipedia.org/wiki/C4.5_algorithm)
- [k-nearest neighbors](https://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm)
- [support vector machine for text data](http://weka.sourceforge.net/doc.dev/weka/classifiers/functions/SGDText.html)

All algorithms used are from the weka library, besides one version of naive bayes,  which we implemented ourselves.

## The Classification Results
For classification we decided to limit our dataset to posts with more than 20 upvotes. This provided us with the best results since the posts where obviously accepted in their community.  
This restricted the ~4.000.000 input documents to nearly 400.000 texts.  
We randomly distributed this data to 95% train and 5% test data.
If not stated differently our own sanitization was used.

|Algorithm   |% correct classified   |
|---|---|
|naive bayes **own implementation**  | 79,88%  |
|naive bayes multinomial text   | 79,67%  |
|SVM for text data with TF * IDF  | 78,4%  |
|SVW   | 77,28%  |
| J48 Tree  | 76,53%  |
| naive bayes multinomial  | 74,52%  |
| bayes net  | 74,5%  |
| naive bayes with TF * IDF | 70,28% |
| k-nearest neighbors  | 69,76%  |
| naive bayes  | 69,52%  |
| naive bayes, [lovins stemmer](http://snowball.tartarus.org/algorithms/lovins/stemmer.html), [rainbow stopwords](http://weka.sourceforge.net/doc.dev/weka/core/stopwords/Rainbow.html) | 68,46%
|  naive bayes, no sanitization | 52%  |

### Conclusion and algorithms used

Important observations from these results for us where first of all, that our own naive bayes implementation was the best and fastest by far. This hast probably to do with the fact, that we could make use of parallel execution and fit the algorithm exactly to our problem.  

One more interesting result was, that the model building process for support vector machines, j48 tree and SVM for text data took more than 12 hours. So we decided that they are not practically for our approach, because we want faster startup times.

So performance wise and from the classification results we chose to use our naive bayes implementation, the bayes net, the k-nearest neighbors and the bayes multinomial for text data algorithms in our running project.



