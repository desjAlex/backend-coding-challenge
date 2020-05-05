# City Directory
This is my submission to the Coveo Backend Coding Challenge, from which this repository is forked. The original text for this challenge is included at the bottom of this ReadMe.

## Deployment
I have hosted the City Directory at https://city-directory.herokuapp.com/, which will bring you to a very simple web interface for making calls to the API in a more "human" capacity.

The API itself is exposed at https://city-directory.herokuapp.com/suggestions, which behaves as instructed by the challenge specification.

I hosted my application on Heroku because it was well-integrated with my IDE (IntelliJ) and language (Java) of choice. Once my environment was setup, I could deploy my application with a single press of a button.

## Language
I selected Java to develop this application for three reasons:

- I'm comfortable working in the language itself, along with its development tools. I didn't want to be restrained by my lack of comfort with something new.
- The language itself is mature and well-suited for this task. It's one of the most commonly used backend languages, and it offers a lot of utility in this context even while ignoring the powerful frameworks that are available.
- Extensions for testing (JUnit), documentation (JavaDoc), and building (Maven) are modern and feature-rich, promoting high quality, maintainable code.

This is the first project I've made using Maven. In the past, I've relied on IDE build management. I decided to use Maven to make this project self-contained and portable, aiming to replicate development in a professional environment. Maven handles all project dependencies and compilation settings, as well as integration with Heroku.

## Implementation

#### API
API calls are managed by an [HttpServlet](/cities/src/main/java/CitySuggestionServlet.java). This is an extension of Java's built-in API support, which makes handling requests trivial.

Requests are parsed, validated, and passed to the data management classes. Results are formatted and returned to the user, or an appropriate status code is generated if an issue arises.

#### Data Management
The [directory](/cities/src/main/java/CityDirectory.java) itself is a singleton object backed by a Radix Tree data structure (described below). It is made thread safe by inclusion of a Readers-Writer lock, which protects against concurrent modification. Since the application is predominantly "read", this enforces data synchronization with minimal overhead.

Source data is parsed using the *Apache Commons CSV library*. Parsing CSV data manually is easy, but there are occasional issues that can arise in malformed data that a robust library can mitigate. Furthermore, this library allows for entries to be parsed by header name, which simplifies my code. Using a library was suitable for this task.

Results are stored in objects precisely formatted for the JSON structure requested in the specification. I used Google's *Gson Library* for serializing and printing these objects with JSON formatting. 

#### Data Structure
I elected not to use a database for this challenge since it felt as though that would trivialize the task. Furthermore, the data itself is barely larger than 1MB, so working in memory rather than on the disk seemed advisable. 

I selected a [Radix Tree](/cities/src/main/java/RadixTree.java) as my backing data structure. This is a type of search tree where values are stored according to a key string. Keys with common prefixes are stored under a common parent node in the tree, which makes this structure well-suited for searching by partial keys. Furthermore, all results can be found in *O(n)* time complexity, where *n* is the length of the key. 

This data structure is not available as a standard Java library, so I implemented it from scratch.

[Learn more about Radix Trees on Wikipedia.](https://en.wikipedia.org/wiki/Trie)

#### Scoring Results
When I parse city data, I also store population (although I never report it to users directly). I use population to modulate how I score results. 

Queries with a latitude and longitude are scored based on distance from the specified position. The value decays from 1 exponentially at a rate that is dictated by that city's population. Cities with larger populations decay slower than smaller ones. I tried to capture the idea that interest in larger cities would extend further, geographically.

Queries with only a name are scored purely based on population. It was difficult to decide on a reasonable method for doing so. In the end, I sum the populations for all cities that were found from this query, and the score for each is a ratio of the logarithm of that cities population to the logarithm of the sum of all the city populations. The drawback for this method is that queries where one city is much larger than the rest will assign that city a very high score. Ignoring the score itself, results are returned in decreasing order of population, which is as straightforward as one can expect for these queries.

I don't report cities with scores lower than 0.1. This is a common occurrence for location-based searching but basic searching will often include most, if not all, the matching results. 

## Conclusion
My mindset for this challenge was to author code with a "professional" approach. I focused on the requirements and met them precisely. I used industry-standard build tools, relied on libraries where appropriate, have good unit test coverage, and included documentation (Javadoc for public methods, regular comments otherwise). 

I implemented an interesting, but still appropriate, data structure so that I could write a reasonable amount of code that would (hopefully) embody these principles. 

In the end, this challenge included a healthy balance of novelty (servlets and APIs, Maven, deployment) and experience. I hope you find it compelling!

[Original challenge specification follows.]

# Coveo Backend Coding Challenge
(inspired by https://github.com/busbud/coding-challenge-backend-c)

## Requirements

Design a REST API endpoint that provides auto-complete suggestions for large cities.

- The endpoint is exposed at `/suggestions`
- The partial (or complete) search term is passed as a querystring parameter `q`
- The caller's location can optionally be supplied via querystring parameters `latitude` and `longitude` to help improve relative scores
- The endpoint returns a JSON response with an array of scored suggested matches
    - The suggestions are sorted by descending score
    - Each suggestion has a score between 0 and 1 (inclusive) indicating confidence in the suggestion (1 is most confident)
    - Each suggestion has a name which can be used to disambiguate between similarly named locations
    - Each suggestion has a latitude and longitude

## "The rules"

- *You can use the language and technology of your choosing.* It's OK to try something new (tell us if you do), but feel free to use something you're comfortable with. We don't care if you use something we don't; the goal here is not to validate your knowledge of a particular technology.
- End result should be deployed on a public Cloud (Heroku, AWS etc. all have free tiers you can use).

## Advice

- **Try to design and implement your solution as you would do for real production code**. Show us how you create clean, maintainable code that does awesome stuff. Build something that we'd be happy to contribute to. This is not a programming contest where dirty hacks win the game.
- Documentation and maintainability are a plus, and don't you forget those unit tests.
- We don’t want to know if you can do exactly as asked (or everybody would have the same result). We want to know what **you** bring to the table when working on a project, what is your secret sauce. More features? Best solution? Thinking outside the box?

## Can I use a database?

If you wish, it's OK to use external systems such as a database, an Elastic index, etc. in your solution. But this is certainly not required to complete the basic requirements of the challenge. Keep in mind that **our goal here is to see some code of yours**; if you only implement a thin API on top of a DB we won't have much to look at.

Our advice is that if you choose to use an external search system, you had better be doing something really truly awesome with it.

## Sample responses

These responses are meant to provide guidance. The exact values can vary based on the data source and scoring algorithm

**Near match**

    GET /suggestions?q=Londo&latitude=43.70011&longitude=-79.4163

```json
{
  "suggestions": [
    {
      "name": "London, ON, Canada",
      "latitude": "42.98339",
      "longitude": "-81.23304",
      "score": 0.9
    },
    {
      "name": "London, OH, USA",
      "latitude": "39.88645",
      "longitude": "-83.44825",
      "score": 0.5
    },
    {
      "name": "London, KY, USA",
      "latitude": "37.12898",
      "longitude": "-84.08326",
      "score": 0.5
    },
    {
      "name": "Londontowne, MD, USA",
      "latitude": "38.93345",
      "longitude": "-76.54941",
      "score": 0.3
    }
  ]
}
```

**No match**

    GET /suggestions?q=SomeRandomCityInTheMiddleOfNowhere

```json
{
  "suggestions": []
}
```

## References

- Geonames provides city lists Canada and the USA http://download.geonames.org/export/dump/readme.txt

## Getting Started

Begin by forking this repo and cloning your fork. GitHub has apps for [Mac](http://mac.github.com/) and
[Windows](http://windows.github.com/) that make this easier.
