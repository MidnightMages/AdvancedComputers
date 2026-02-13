using System.Text.RegularExpressions;

namespace GenerateApiDocs;

internal partial class Program {
    private static void Main(string[] args) {
        var javaSourcesRootPath = Path.GetFullPath(Path.Combine([AppContext.BaseDirectory, .. Enumerable.Repeat("..", 6), "src", "main", "java"]));

        string[] luaAttribNames = ["LuaCallable", "LuaExposed"];

        foreach (var javaFilePath in Directory.EnumerateFiles(javaSourcesRootPath, "*.java", SearchOption.AllDirectories).Select(x => x.Replace('\\', '/'))) {
            var rawFileContents = File.ReadAllText(javaFilePath);
            string fileContents = RemoveComments(rawFileContents);

            var attribNamesConcatted = string.Join("|", luaAttribNames);
            var regexMatches = Regex.Matches(fileContents, $@"(@(?:{attribNamesConcatted})\S*)\s*([^\n]+)", RegexOptions.Multiline);

            if (regexMatches.Any()) {
                string javaFile = javaFilePath.Substring(javaSourcesRootPath.Length + 1);
                string hrule = new string('=', 10);
                Console.WriteLine($"{hrule} {javaFile} {hrule}");
                foreach (Match match in regexMatches) {
                    //var splitted = match.ToString()!.Split('\n', StringSplitOptions.TrimEntries);
                    var attrib = match.Groups[1].ToString().Trim();
                    var javaHeader = match.Groups[2].ToString().TrimEnd(' ', '{', '\r');
                    Console.WriteLine($"{attrib}:\n\t{javaHeader}\n");
                }
            }
        }

        Console.WriteLine();
    }


    public static string RemoveComments(string contents) {
        // remove comments
        var fileContentsNoLineComments = RegexCommentPattern().Replace(contents, ""); // replace end-of-line comments
        return RegexMultilineCommentPattern().Replace(fileContentsNoLineComments, ""); // replace multiline comments
    }

    [GeneratedRegex("//.*?$", RegexOptions.Multiline)]
    private static partial Regex RegexCommentPattern();
    [GeneratedRegex(@"/\*.*?\*/", RegexOptions.Multiline)]
    private static partial Regex RegexMultilineCommentPattern();
}
