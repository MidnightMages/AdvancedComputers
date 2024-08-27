using namespace System.Drawing.Imaging
Add-Type -Assembly System.Drawing
$ErrorActionPreference = "Stop"

foreach ($subfolder in @("device")){
	$basefolder = "./assetSources/textures/block/tcable/"
	$infolder = $basefolder+$subfolder+"/"
	$outfolder = $basefolder+$subfolder+"_red/"
	$a = New-Item -ItemType "directory" -Path $outfolder -Force
	$inImgs = Get-ChildItem $infolder -Filter *.png
	foreach ($imgFilename in $inImgs) {
		$fullInImgPath = $infolder + $imgFilename
		$fullOutImgPath = $outfolder+$imgFilename
		$img = New-Object System.Drawing.Bitmap $fullInImgPath
		$outimg = New-Object System.Drawing.Bitmap($img.Width, $img.Height)
		$thr = 255
		foreach ($x in 0..($img.Width - 1)) {
			foreach ($y in 0..($img.Height - 1)) {
				$color = $img.GetPixel($x, $y)
				if($color.R -ge $thr -and $color.G -ge $thr -and $color.B -ge $thr){
					$color = [System.Drawing.Color]::FromArgb(255, 0, 0)
				}
				$outimg.SetPixel($x, $y, $color)
			}
		}
		
		#echo $fullOutImgPath
		$outimg.Save($fullOutImgPath, [System.Drawing.Imaging.ImageFormat]::Png)
	}
}