#!/usr/bin/env python
import re, sys, operator

# Mileage may vary. If this crashes, make it lower
RECURSION_LIMIT = 9500
# We add a few more, because, contrary to the name,
# this doesn't just rule recursion: it rules the 
# depth of the call stack
sys.setrecursionlimit(RECURSION_LIMIT+10)

# Define the Y-combinator (immediately from https://github.com/crista/EPS-slides/blob/master/lambda.py)
Y = (lambda h: lambda F: F(lambda x: h(h)(F)(x)))(lambda h: lambda F: F(lambda x: h(h)(F)(x)))


def count(word_list, stopwords, wordfreqs):
    fixed_point = lambda f: lambda wl: lambda sws: lambda wfs: None if wl == [] else f(wl[1:])(sws)(wfs) if wfs.update({wl[0]: (wfs.get(wl[0], 0) + (1 if wl[0] not in sws else 0))}) is None else True
    Y(fixed_point)(word_list)(stopwords)(wordfreqs)


def wf_print_y(wordfreq):
    Y(lambda f: lambda wfs: None if wfs == []
      else f(wfs[1:]) if print(f"{wfs[0][0]}  -  {wfs[0][1]}") is None
      else None)(wordfreq)


# small test case
# wf_print_y([('a', 1), ('b', 2), ('c', 3)])

stop_words = set(open('../stop_words.txt').read().split(','))
words = re.findall('[a-z]{2,}', open(sys.argv[1]).read().lower())
word_freqs = {}
# Theoretically, we would just call count(words, word_freqs)
# Try doing that and see what happens.
# count(words, stop_words, word_freqs)

for i in range(0, len(words), RECURSION_LIMIT):
    count(words[i:i+RECURSION_LIMIT], stop_words, word_freqs)

wf_print_y(sorted(word_freqs.items(), key=operator.itemgetter(1), reverse=True)[:25])



# ======================================================================================================================
# Original main below

# stop_words = set(open('../stop_words.txt').read().split(','))
# words = re.findall('[a-z]{2,}', open(sys.argv[1]).read().lower())
# word_freqs = {}
# # Theoretically, we would just call count(words, word_freqs)
# # Try doing that and see what happens.
# for i in range(0, len(words), RECURSION_LIMIT):
#     count(words[i:i+RECURSION_LIMIT], stop_words, word_freqs)
#
# wf_print(sorted(word_freqs.iteritems(), key=operator.itemgetter(1), reverse=True)[:25])

# ======================================================================================================================

# ======================================================================================================================
# finding "fixed point" of the function:
# x such that x = f(x) <- fixed point of f
# g such that g = f(g) for functionals (functions that take a function as argument)
# ======================================================================================================================
