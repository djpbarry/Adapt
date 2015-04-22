The ADAPT package is based largely around the Analyse_Movie class, the workhorse of the software, implementing ImageJ's [PlugIn interface](http://rsbweb.nih.gov/ij/developer/api/index.html).

Invoking the Analyse_Movie.run method will cause ADAPT to run as it would when selected from ImageJ's pluging menu, complete with GUI for the specification of parameters.

Alternatively, a constructor exists to initiate an instance of the Analyse_Movie object with an instance of the UserVariables class, obviating the need to display a GUI. This constructor also allows the specification of other optional parameters.

There also exists a subclass of Analyse_Movie, Analyse_Batch, which, as the name suggests, permits the analysis of a series of images. Using the default constructor and then invoking the run method will open dialogs for the specification of input directories and analysis parameters. Alternatively, there is a second constructor which allows the specification of these parameters, such that batch mode can be invoked from the command line or from another application.

For further information on any of ADAPT's functions, consult the API, which is available to [download](https://bitbucket.org/djpbarry/adapt/downloads).