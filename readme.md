Hello World plugin example for the cliforce shell

run mvn install

then run the shell and

force> plugin HelloWorld cliplugin:cliplugin:1.0

You have installed the plugin, you can now

>force hello

and get a hello back

You can uninstall the plugin like this

>force unplug HelloWorld

For Example...

force> plugin HelloWorldPlugin cliplugin:cliplugin:1.0
Adding Plugin: HelloWorld, (HelloWorldPlugin)
  -> adds command hello, (HelloWorldPlugin$HelloWorldCommand)
force> hello
hello
Hello World
force> unplug HelloWorld
unplug HelloWorld
attempting to remove plugin: HelloWorld
removed command: hello

Done
force>