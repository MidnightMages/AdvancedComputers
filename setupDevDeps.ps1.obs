function CopyAndExtract {
    param (
        $package,
        $version,
        $name,
        $nameSuffix
    )

    $realPackage = $package.Replace('.', '/')
    $jarName = "$name-$version$nameSuffix"

    if (-Not (Test-Path "$PSScriptRoot\build\tmp\downloads")) {
        Write-Output "creating tempdir"
        MKDIR "$PSScriptRoot\build\tmp\downloads" | Out-Null
    }

    if (-Not (Test-Path "$PSScriptRoot\build\tmp\luajava\$name.zip")) {
        Write-Output "downloading $jarName.jar"
        Invoke-WebRequest "https://repo.maven.apache.org/maven2/$realPackage/$name/$version/$jarName.jar" -OutFile "$PSScriptRoot\build\tmp\downloads\$jarName.zip"
    }

    # explode jar into compiled classes
    Write-Output "extracting $jarName.jar"

    Expand-Archive -LiteralPath "$PSScriptRoot\build\tmp\downloads\$jarName.zip" -DestinationPath "$PSScriptRoot\build\classes\java\main" -Force
    Remove-Item -Recurse -Force "$PSScriptRoot\build\classes\java\main\META-INF"
}

$luaVersion = "3.5.0"
$luaJavaPgk = "party.iroiro.luajava"

CopyAndExtract $luaJavaPgk $luaVersion "luajava"
CopyAndExtract $luaJavaPgk $luaVersion "lua54"
CopyAndExtract $luaJavaPgk $luaVersion "lua54-platform" "-natives-desktop"
CopyAndExtract "com.badlogicgames.gdx" "2.3.1" "gdx-jnigen-loader"
