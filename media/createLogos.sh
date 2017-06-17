#!/bin/sh

INFILE="logo_hq.png"
SMALLICON="logo_icon.png"
ROUNDICON="logo_hq_round.png"

mkdir -p mipmap-xxxhdpi mipmap-xxhdpi mipmap-xhdpi mipmap-mdpi mipmap-hdpi

convert $INFILE -resize 192x192 mipmap-xxxhdpi/ic_launcher.png
convert $INFILE -resize 144x144 mipmap-xxhdpi/ic_launcher.png
convert $INFILE -resize 96x96 mipmap-xhdpi/ic_launcher.png
convert $INFILE -resize 48x48 mipmap-mdpi/ic_launcher.png
convert $INFILE -resize 72x72 mipmap-hdpi/ic_launcher.png
convert $INFILE -resize 512x512 logo_hq_512.png

convert $SMALLICON -resize 192x192 mipmap-xxxhdpi/smallicon.png
convert $SMALLICON -resize 144x144 mipmap-xxhdpi/smallicon.png
convert $SMALLICON -resize 96x96 mipmap-xhdpi/smallicon.png
convert $SMALLICON -resize 48x48 mipmap-mdpi/smallicon.png
convert $SMALLICON -resize 72x72 mipmap-hdpi/smallicon.png

convert $ROUNDICON -resize 192x192 mipmap-xxxhdpi/ic_launcher_round.png
convert $ROUNDICON -resize 144x144 mipmap-xxhdpi/ic_launcher_round.png
convert $ROUNDICON -resize 96x96 mipmap-xhdpi/ic_launcher_round.png
convert $ROUNDICON -resize 48x48 mipmap-mdpi/ic_launcher_round.png
convert $ROUNDICON -resize 72x72 mipmap-hdpi/ic_launcher_round.png

