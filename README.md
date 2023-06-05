# Working video player in minecraft

This plugin allows you to play videos/streams and paste images in Minecraft. It uses new thread to process videos/streams, so it can work with any fps you want assuming you have the processing power. I managed to make it work in 60fps (I'm sure i can go further) with 20tps in a Ryzen 5 5600x (No overclocking).

## How to use

- /processvideo [url]
- /processimage [url]
- /processstream
- /setres [width] [height] [fps] _(Only FPS changes dynamically, so you can change it while the video is playing. Width and height changes only when you process a new video/image)_

### How to setup streaming

- Documentation coming soon

## How to build

1. Clone the repository
2. Build it using `mvn package -Djavacpp.platform=(your platform)` ex: `mvn package -Djavacpp.platform=linux-x86_64` for more information check [javacv](https://github.com/bytedeco/javacpp-presets#downloads).

## How to install

1. Download the plugin from [releases](https://github.com/DarkSavci/minecraft-video-player/releases)
2. Put the plugin in your plugins folder
3. Start the server (Should only work in 1.19.4 but i didn't tested it in other versions)

# TO-DO

- [ ] Add config file to change the max fps and max resolution
