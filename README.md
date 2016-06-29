# The Political Opinion Classifier

This is a project by Yannick Gladow and Simon Wanner.
The goal is to correctly classify political opinion based on data from reddit.

## The Idea
basic idea

## The Data
Reddit etc

## The Software Architecture
[The Importer](https://github.com/yannick-cw/poc-importer)  
[The Frontend](https://github.com/yannick-cw/poc_frontend)  
[The cleaner](https://github.com/yannick-cw/poc_cleaner)  

docker, microservices, scala, elasticsearch, weka, play  
![structure](https://github.com/yannick-cw/poc_analyzer/blob/master/pocStructure.png "Architecture")

## The Algorithms used
algortithms in running application

## The Classification Results

Input is 100.000 documents with more than 20 upvotes.  
5% random data of that is used as testdata.

#### Naive Bayes

###### With Weka implementation of naive bayes
- own sanitization with TF * IDF: 70.28%
- own sanitization without TF * IDF: 69.52%
- raw input text without sanitization, without IDFTransform: 52%
- raw input text with LovinsStemmer, Rainbow Stopwords, without TF * IDF: 68.46%
- raw input text with LovinsStemmer, Rainbow Stopwords, with TF * IDF: 68% 


###### With own naive bayes implementation  
- own sanitization, no TF * IDF: 79.88%
and waaay faster

#### BayesNet
- own sanitization with TF * IDF: 74.5%

#### NaiveBayes Multinomial
- own sanitization with TF * IDF: 74.52%

#### NaiveBayesMultinomialText
- own sanitization without TF * IDF: 79.67%

#### Support Vector Machine
- really long training time
- own sanitization without TF * IDF: 77.28%

#### J48 Tree
- really long training time
- own sanitization with TF * IDF: 76.53%

#### k-Nearest Neighbors
- own sanitization with TF * IDF: 69.76%

#### SVM for text data
- own sanitization with TF * IDF: 78.4%
