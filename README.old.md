<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[Contributors][contributors-url]
·
[Forks][forks-url]
·
[Issues][issues-url]
·
[License][license-url]

<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/raw/develop/images/logo.png">
    <img src="https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/raw/develop/images/logo.png" alt="Logo" width="300" height="125">
  </a>

  <h3 align="center">Corpus Services</h3>

  <p align="center">
    The Corpus Services project bundles functionality used for maintenance, curation, conversion, and visualization of corpus data in various projects. 
    <br />
    <a href="https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/issues">Report Bug</a>
    ·
    <a href="https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/issues">Request Feature</a>
  </p>
</p>



<!-- TABLE OF CONTENTS -->
## Table of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
* [Usage](#usage)
* [Roadmap](#roadmap)
* [Contributing](#contributing)
* [License](#license)
* [Authors](#authors)
* [Contact](#contact)
* [Acknowledgements](#acknowledgements)



<!-- ABOUT THE PROJECT -->
## About The Project

The (HZSK) Corpus Services were initially developed at the Hamburg Centre for Language Corpora (HZSK) as a quality control and publication framework for [EXMARaLDA](https://exmaralda.org/en/) corpora. Since then, most development work has been done within the [INEL](https://inel.corpora.uni-hamburg.de) project. A focus has been set on making the code adaptable to other use cases and data types.
The Corpus Services project now bundles functionality used for maintenance, curation, conversion, and visualization of corpus data in various projects. 

<!-- GETTING STARTED -->
## Getting Started

Additional documentation on the Corpus services can be found in the doc folder:

* [How to use](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/How_to_use.md)
* [List of corpus functions](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/List_of_corpus_functions.md)
* [How to add a new function](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/How-to_add_a_new_function.md)
* [Build with Maven](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/Build_with_Maven.md)

You can also find some sample scripts (batch and shell) to use for calls to the corpus services jar and some further utilities [here](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/utilities).

There is also some information and scripts useful for automating the use of corpus-services available [here](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/cubo).

### Prerequisites

Java needs to be installed. 

#### Gitlab artifacts

The latest compiled .jar file can be found [here](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/jobs/artifacts/develop/browse?job=compile_withmaven).

#### Building

To use the services for corpora, compile it using `mvn clean compile assembly:single`.
(See [Build with Maven](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/Build_with_Maven.md)
or use a pregenerated artifact from Gitlab that you can download [here](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/jobs/artifacts/develop/browse?job=compile_withmaven). 

<!-- USAGE EXAMPLES -->
## Usage

The usable functions can be found in the help output:

`java -jar corpus-services-1.0.jar -h`

See [How to use](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/How_to_use.md) for the usage of the corpus services.
 
<!-- LIBRARIES -->
## Libraries

* [EXMARaLDA](https://github.com/Exmaralda-Org/exmaralda)
* [Doxygen](https://www.doxygen.nl)

<!-- ROADMAP -->
## Roadmap

See the [open issues](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/issues) for a list of proposed features (and known issues).


<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request



<!-- LICENSE -->
## License

Distributed under MIT License. See `LICENSE` for more information.


<!-- AUTHORS -->
## Authors

Anne Ferger

Hanna Hedeland

Daniel Jettka

Tommi Pirinen

<!-- CONTACT -->
## Contact

Anne Ferger - [@anneferger1](https://twitter.com/anneferger1) - anne.ferger@uni-hamburg.de

Project Link: [Corpus Services](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services)


## Metadata

PID: http://hdl.handle.net/11022/0000-0007-D8A6-A

## Citation

For an introduction to the system please cite 

Hedeland, H. & Ferger, A. (2020). Towards Continuous Quality Control for Spoken Language Corpora. International Journal for Digital Curation, 15(1). https://doi.org/10.2218/ijdc.v15i1.601 

[comment]: # (for more recent information please refer to ...)


<!-- ACKNOWLEDGEMENTS -->
## Acknowledgements

Contributions to the project have been made by staff from the HZSK and several research projects at the University of Hamburg: [INEL](https://inel.corpora.uni-hamburg.de), the BMBF-funded CLARIN-D project (01UG1620G), the project WO 1886/1-2 within the DFG LIS program, and the BMBF-funded project [QUEST](https://www.slm.uni-hamburg.de/ifuu/forschung/forschungsprojekte/quest.html).

<sub>Parts of this project have been produced in the context of the joint research funding of the German Federal Government and Federal States in the Academies’ Programme, with funding from the Federal Ministry of Education and Research and the Free and Hanseatic City of Hamburg. The Academies’ Programme is coordinated by the Union of the German Academies of Sciences and Humanities.</sub>

Thank you to all funders, supporters and [contributors][contributors-url]!

Logo created at [LogoMakr.com](https://logomakr.com/).
This README file was created on the basis of the [Best-README-Template](https://github.com/othneildrew/Best-README-Template/blob/master/README.md).

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/othneildrew/Best-README-Template.svg?style=flat-square
[contributors-url]: https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/graphs/develop
[forks-url]: https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/forks
[issues-url]: https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/issues
[license-url]: https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/blob/develop/LICENSE

