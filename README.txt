# turtle - load readable_model from vowpal wabbit and serve it from java

## example train a model

$ vw -d /path/to/dataset/dataset --readable_model readable_model.txt

##  readable_model.txt looks like:
...
6163:0.0624969
7472:-0.12023
42847:-0.0421919
49960:-0.12023
51692:0.0624969
55432:-0.0421919
...

using https://gist.github.com/luoq/b4c374b5cbabe3ae76ffacdac22750af,
and some logic to produce the same buckets for the same feature combinations

## java code
ReadableModel m = new ReadableModel("directory/of/readable_model.txt");


// this will load the model creating only float[] array with same size as 2**bits(-b from vw)
// it uses same algorithm to compute hash buckets as vw and then returns the inner product

m.predict(
          new Doc(
              new Namespace(
                  "your-namespace",
                  new Feature("a"),
                  new Feature("b"),
                  new Feature("c"))));

## make sure it works
if you want to make sure your parameters are supported, in the repeatable_model.txt
add test.txt and predictions.txt (using -p from vw) and it will automatically
test if turtle gets the same predictions as the wabbit.

check out resources/test for more examples

## thanks

* Lucas Bernardi
* Tarek Sheasha
* Denis Bilenko

wouldn't have happened without the help of those guys

## todo

* more tests
* verify the checksum of the model
* support for --cubic
* support for ngrams and skips
* support for --lrq
