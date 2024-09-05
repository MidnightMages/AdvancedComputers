$dstFolders = @("textures","models")
$dstPrefixPath = "./src/main/resources/assets/advancedcomputers/"
foreach ($suffix in $dstFolders){
	$pathToDel = $dstPrefixPath+$suffix
	if (Test-Path $pathToDel){
		Remove-item  -Recurse -Force $pathToDel
	}
}

& "./createTintedCableTextures.ps1"

Copy-Item -Path "./assetSources/textures/" -Destination "./src/main/resources/assets/advancedcomputers/textures/" -Recurse -Filter '*.png' -Force
Copy-Item -Path "./assetSources/bbmodels/wan_router/*" -Destination "./src/main/resources/assets/advancedcomputers/textures/block/" -Recurse -Filter '*.png' -Force
New-Item -ItemType Directory -Path "./src/main/resources/assets/advancedcomputers/models/block/" | Out-Null
Copy-Item -Path "./assetSources/bbmodels/wan_router/*" -Destination "./src/main/resources/assets/advancedcomputers/models/block/" -Recurse -Filter '*.json' -Force