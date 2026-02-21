global using static SharedLib.GlobalUsings;
using System.Diagnostics;
using System.Text.RegularExpressions;

namespace BuildTool;

internal class Program {

    private record struct SemVer(int Major, int Minor, int Patch, string Suffix) {
        public static SemVer Parse(string s) {
            var trimmed = s.Trim();
            var splitted = trimmed.Split('-');
            Assert(splitted.Length == 2);
            var versionNumbers = splitted[0].Split(".").Select(int.Parse).ToArray();
            Assert(versionNumbers.Length == 3);
            return new SemVer(versionNumbers[0], versionNumbers[1], versionNumbers[2], splitted[1]);
        }

        public override readonly string ToString() {
            return $"{Major}.{Minor}.{Patch}-{Suffix}";
        }

        public SemVer WithBumpedMajor() => new SemVer(Major + 1, 0, 0, Suffix);
        public SemVer WithBumpedMinor() => new SemVer(Major, Minor + 1, 0, Suffix);
        public SemVer WithBumpedPatch() => new SemVer(Major, Minor, Patch + 1, Suffix);
    }

    private static async Task<string> StartProcessAndGetStdOutAsync(string executableAndArgs) {
        var splitted = executableAndArgs.Split(' ', 2);
        using var proc = new Process() {
            StartInfo = new ProcessStartInfo(splitted[0], splitted[1]) {
                UseShellExecute = false,
                RedirectStandardOutput = true,
            },
        };
        proc.Start();
        await proc.WaitForExitAsync();
        Assert(proc.ExitCode == 0);
        return await proc.StandardOutput.ReadToEndAsync();
    }

    private static async Task Main(string[] args) {
        Console.WriteLine("Ac Build Tool");
        var acRepoSourcePath = Path.GetFullPath(Path.Combine([AppContext.BaseDirectory, .. Enumerable.Repeat("..", 6)]));
        var gradlePropertiesPath = Path.Combine(acRepoSourcePath, "gradle.properties");
        Assert(File.Exists(gradlePropertiesPath), "couldnt find gradle.properties");
        var currentVersionRaw = File.ReadAllText(gradlePropertiesPath).ReplaceLineEndings("\n").Split('\n').Single(x => x.StartsWith("mod_version=")).Split('=', 2)[1];
        SemVer currentVersion = SemVer.Parse(currentVersionRaw);

        var doWeHaveUncommittedChanges = (await StartProcessAndGetStdOutAsync("git status -s")).Length != 0;

        if (doWeHaveUncommittedChanges) {
            Console.WriteLine("We have uncommitted changes - aborting. Commit your changes first.");
            Environment.Exit(1);
        }

        Console.WriteLine($"""
            Current version: {currentVersion}
            Which version number do we bump? 
            [M]ajor: {currentVersion.WithBumpedMajor()}
            [m]inor: {currentVersion.WithBumpedMinor()} 
            [p]atch: {currentVersion.WithBumpedPatch()}

            """);

        char input;
        while (!"Mmp".Contains(input = Console.ReadKey(true).KeyChar)) { Console.WriteLine("Input must be one of 'M' 'm' or 'p'"); }
        SemVer newVersion = input switch {
            'M' => currentVersion.WithBumpedMajor(),
            'm' => currentVersion.WithBumpedMinor(),
            _ => currentVersion.WithBumpedPatch()
        };
        Console.WriteLine($"New version: {newVersion}\n" +
            $"Should we really continue and create a commit, git tag and trigger a build? y/n");


        char input2;
        while (!"yn".Contains(input2 = Console.ReadKey(true).KeyChar)) { Console.WriteLine("Input must be one of 'y' or 'n'"); }


        if (input2 != 'y') {
            Console.WriteLine("Aborted.");
            Environment.Exit(2);
        }
        Console.WriteLine("Modifying gradle.properties...");
        var gradlePropertiesTextOriginal = File.ReadAllText(gradlePropertiesPath);
#pragma warning disable SYSLIB1045 // Convert to 'GeneratedRegexAttribute'.
        var gradlePropertiesText = Regex.Replace(gradlePropertiesTextOriginal, "^mod_version=.*?$", $"mod_version={newVersion}", RegexOptions.Multiline);
#pragma warning restore SYSLIB1045 // Convert to 'GeneratedRegexAttribute'.
        await File.WriteAllTextAsync(gradlePropertiesPath, gradlePropertiesText);

        Console.WriteLine("Staging...");
        await StartProcessAndGetStdOutAsync("git add -A");
        Console.WriteLine("Committing...");
        await StartProcessAndGetStdOutAsync("git commit -m \"bump version\"");
        Console.WriteLine("Creating tag...");
        var newTagName = $"{newVersion.Suffix}/{newVersion.Major}.{newVersion.Minor}.{newVersion.Patch}";
        await StartProcessAndGetStdOutAsync($"git tag {newTagName}");
        Console.WriteLine("Starting new build...");
        using var proc = new Process() {
            StartInfo = new ProcessStartInfo("cmd.exe", $"/c \"{Path.Combine(acRepoSourcePath, "createReleaseBuild.bat")}\"") {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardError = true,
                RedirectStandardOutput = true,
                WorkingDirectory = acRepoSourcePath
            },
        };
        proc.Start();

        int buildSuccessCount = 0; // how often we saw "BUILD SUCCESSFUL", which should be 2 if all worked out (datagen + build)
        object lockobj = new();
        proc.OutputDataReceived += (object _, DataReceivedEventArgs e) => {
            lock (lockobj) {
                Console.WriteLine($"OUT->{e.Data}");
                if (e.Data?.Contains("BUILD SUCCESSFUL in") == true) {
                    buildSuccessCount++;
                }
            }
        };

        proc.ErrorDataReceived += (object _, DataReceivedEventArgs e) => {
            lock (lockobj) {
                var fg = Console.ForegroundColor;
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"ERR->{e.Data}");
                Console.ForegroundColor = fg;
            }
        };

        proc.BeginOutputReadLine();
        proc.BeginErrorReadLine();
        await proc.WaitForExitAsync();
        Assert(proc.ExitCode == 0, "Nonzero exit code on build");
        Assert(buildSuccessCount == 2, "A build seems to have failed");
        Console.WriteLine("Build complete. Pushing git branch...");
        await StartProcessAndGetStdOutAsync("git push origin");
        Console.WriteLine("Pushing git tag...");
        await StartProcessAndGetStdOutAsync($"git push origin tag {newTagName}");
        Console.WriteLine("All done!");
    }
}