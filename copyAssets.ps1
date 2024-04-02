$dstPath = "./src/main/resources/assets/advancedcomputers/textures"
if (Test-Path $dstPath){
	Remove-item  -Recurse -Force  $dstPath
}

Copy-Item -Path "./assetSources/textures/" -Destination "./src/main/resources/assets/advancedcomputers/textures/" -Recurse -Filter '*.png' -Force