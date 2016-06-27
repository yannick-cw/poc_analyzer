## The Political Opinion Classifier

This is a project by Yannick Gladow and Simon Wanner.
The goal is to correctly classify political opinion based on data from reddit.

### Classification Results

#### Naive Bayes
Input is 100.000 documents with more than 20 upvotes.  
5% random data of that is used as testdata.

With Weka implementation of naive bayes
- own sanitization with TF * IDF: 70.28%
- own sanitization without TF * IDF: 69.52%
- raw input text without sanitization, without IDFTransform: 52%
- raw input text with LovinsStemmer, Rainbow Stopwords, without TF * IDF: 68.46%
- raw input text with LovinsStemmer, Rainbow Stopwords, with TF * IDF: 68% 

--------------------

With own naive bayes implementation
- own sanitization: 79.88%
- and waaay faster

#### 