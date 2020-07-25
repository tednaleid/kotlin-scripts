# Kotlin Scripts

Simple kotlin scripts that can be executed directly as long as you have `kotlinc` in your path.

If you don't it can be installed via [sdkman](https://sdkman.io/install):

```bash
curl -s "https://get.sdkman.io" | bash
sdk install kotlin
```

Then, scripts can be run directly and should all have the `--help` flag, ex:

```bash
scripts/hello.main.kts --help
Usage: hello [OPTIONS]

  try: echo "Ted\nWorld" | scripts/hello.main.kts --count 3

  Help lines that keep their newlines:

  one
  two

Options:
  --count INT   a number of times to say hello to each line, default 2
  --input FILE  input, defaults to stdin
  -h, --help    Show this message and exit
```

and actually run: 

```bash
echo "World" | scripts/hello.main.kts --count 3
Hello World
Hello World
Hello World
```

# "shebang" for kotlin scripts

Using this, won't work quite like we want it to because it won't support command line switches without 
requiring that the user add a `--` before any flags/arguments that should be passed to the script:

```shell script
#!/usr/bin/env kotlin
```

On newer systems, including recent versions of Mac OSX (any on coreutils >= 8.30), the `/usr/bin/env` 
command supports a `-S` flag that will let you pass multiple arguments to env.  Without this switch,
it will try to search your path for a command that matches the entire string.

```shell script
#!/usr/bin/env -S kotlinc -script --
```

On older systems, an alternative would be to do:

```shell script
#!/bin/sh
//usr/bin/env true; exec kotlinc -script "$0" -- "$@"  # better than #!/usr/bin/env kotlin as that doesn't support switches without "--" prefix
```

This is an old unix hack that's really writing a script that's both interpreted by the shell as well as by the JVM.

The `//usr/bin/env true;` gets reduced down to a call to `true` which always succeeds, then `exec` is a shell command
that replaces the current process with the underlying command.  This should be valid for all POSIX shells, but it's
also uglier.