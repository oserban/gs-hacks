# gs-hacks
This a collection of Graphstream Hacks, used in various personal projects

The code adds a custom action listener to Graphstream (which can be further extended, if needed) plus various useful actions:

- navigation buttons
- zoom in/out and reset zoon buttons
- zoom in/out on middle scroll 
- Pan on mouse click

## Build

The project is developed using ant and ivy. To initialise the project execute: 

```bash
ant getDependencies
```

This will download all libraries into the **lib/jar** folder.

To build the jar, you can execute:

```bash
ant release
```

The newly built jars will be available in the **prod** folder.

## Example

![gs-hacks](https://cloud.githubusercontent.com/assets/3008878/16011823/d7206d4e-317e-11e6-966c-ef99a040b0ba.png)

This example is available in the ExampleUI class.
