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