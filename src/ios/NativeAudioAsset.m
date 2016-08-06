//
// 
//  NativeAudioAsset.m
//  NativeAudioAsset
//
//  Created by Sidney Bofah on 2014-06-26.
//

#import "NativeAudioAsset.h"

@implementation NativeAudioAsset

static const CGFloat FADE_STEP = 0.05;
static const CGFloat FADE_DELAY = 0.08;

-(id) initWithPath:(NSString*) path withVoices:(NSNumber*) numVoices withVolume:(NSNumber*) volume withFadeDelay:(NSNumber *)delay
{
    self = [super init];

    if(self) {
        voices = [[NSMutableArray alloc] init];  
        
        NSURL *pathURL = [NSURL fileURLWithPath : path];
        
        for (int x = 0; x < [numVoices intValue]; x++) {
            AVAudioPlayer *player = [[AVAudioPlayer alloc] initWithContentsOfURL:pathURL error: NULL];
            player.volume = volume.floatValue;
            [player prepareToPlay];
            [voices addObject:player];
            [player setDelegate:self];
            
            if(delay)
            {
                fadeDelay = delay;
            }
            else {
                fadeDelay = [NSNumber numberWithFloat:FADE_DELAY];
            }
            
            initialVolume = volume;
        }
        
        playIndex = 0;
    }
    return(self);
}

-(id) initWithUrl:(NSString*) url withVoices:(NSNumber*) numVoices withVolume:(NSNumber*) volume withFadeDelay:(NSNumber *)delay
{
    self = [super init];

    if(self) {
        voices = [[NSMutableArray alloc] init];  
        
        NSURL *pathURL = [NSURL URLWithString:url];
        NSData *data = [NSData dataWithContentsOfURL:pathURL];
        for (int x = 0; x < [numVoices intValue]; x++) {
    
            AVAudioPlayer *player = [[AVAudioPlayer alloc] initWithData:data error:NULL];
            player.volume = volume.floatValue;
            [player prepareToPlay];
            [voices addObject:player];
            [player setDelegate:self];
            
            if(delay)
            {
                fadeDelay = delay;
            }
            else {
                fadeDelay = [NSNumber numberWithFloat:FADE_DELAY];
            }
            
            initialVolume = volume;
        }
        playIndex = 0;
    }
    return(self);
}




- (void) play
{
    AVAudioPlayer * player = [voices objectAtIndex:playIndex];
    [player setCurrentTime:0.0];
    player.numberOfLoops = 0;
    [player play];
    
    playIndex += 1;
    playIndex = playIndex % [voices count];
}


// The volume is increased repeatedly by the fade step amount until the last step where the audio is stopped.
// The delay determines how fast the decrease happens
- (void)playWithFade
{
    AVAudioPlayer * player = [voices objectAtIndex:playIndex];
    
    if (!player.isPlaying)
    {
        [player setCurrentTime:0.0];
        player.numberOfLoops = 0;
        player.volume = 0;
        [player play];
        playIndex += 1;
        playIndex = playIndex % [voices count];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self performSelector:@selector(playWithFade) withObject:nil afterDelay:fadeDelay.floatValue];
        });
    }
    else
    {
        if(player.volume < initialVolume.floatValue)
        {
            player.volume += FADE_STEP;
            dispatch_async(dispatch_get_main_queue(), ^{
                [self performSelector:@selector(playWithFade) withObject:nil afterDelay:fadeDelay.floatValue];
            });
        }
    }
}

- (void) stop
{
    for (int x = 0; x < [voices count]; x++) {
        AVAudioPlayer * player = [voices objectAtIndex:x];
        [player stop];
    }
}

- (void)_fadeTo
{
    BOOL shouldContinue = NO;
    CGFloat fstep = 0;


    if (fhandler == nil) {
        return;
    }

    for (int x = 0; x < [voices count]; x++) {
        AVAudioPlayer * player = [voices objectAtIndex:x];
        fstep = (fto - player.volume)/fsteps;
        if (player.isPlaying && fsteps > 0 && fabsf(fto - player.volume) > 0) {
            player.volume += fstep;
            // NSLog([NSString stringWithFormat:@"FTO %0.2F STEP: %0.2f VOL: %0.2f STEPS: %d", fto, fstep, player.volume, fsteps]);
            fsteps--;
            shouldContinue = YES;
        } 
    }


    if (shouldContinue) {
        [self performSelector:@selector(_fadeTo) withObject:nil afterDelay:0.05];
    } else if (fhandler != nil) {
        fhandler(true);
        // [fhandler release];
        fhandler = nil;
    }
         
}

- (void)fadeTo:(NSNumber*) to duration:(NSNumber *)duration handler: (void(^)(bool))handler
{
    
    if (fhandler) {
        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(_fadeTo) object:nil];
        fhandler(false);
        // [fhandler release];
        fhandler = nil;
    }
    fto = to.floatValue;
    fduration = duration.floatValue;
    fsteps = floor(fduration/50);

    fhandler = [handler copy];
    [self _fadeTo];
}

// The volume is decreased repeatedly by the fade step amount until the volume reaches the configured level.
// The delay determines how fast the increase happens
- (void)stopWithFade
{
    BOOL shouldContinue = NO;
    
    for (int x = 0; x < [voices count]; x++) {
        AVAudioPlayer * player = [voices objectAtIndex:x];
        
        if (player.isPlaying && player.volume > FADE_STEP) {
            player.volume -= FADE_STEP;
            shouldContinue = YES;
        } else {
            // Stop and get the sound ready for playing again
            [player stop];
            player.volume = initialVolume.floatValue;
            player.currentTime = 0;
        }
    }
    
    if(shouldContinue) {
        [self performSelector:@selector(stopWithFade) withObject:nil afterDelay:fadeDelay.floatValue];
    }
}

- (void) loop
{
    [self stop];
    AVAudioPlayer * player = [voices objectAtIndex:playIndex];
    [player setCurrentTime:0.0];
    player.numberOfLoops = -1;
    [player play];
    playIndex += 1;
    playIndex = playIndex % [voices count];
}

- (void) unload 
{
    [self stop];
    for (int x = 0; x < [voices count]; x++) {
        AVAudioPlayer * player = [voices objectAtIndex:x];
        player = nil;
    }
    voices = nil;
}

- (void) setVolume:(NSNumber*) volume;
{
    if (fhandler) {
        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(_fadeTo) object:nil];
        fhandler(false);
        fhandler = nil;
    }

    for (int x = 0; x < [voices count]; x++) {
        AVAudioPlayer * player = [voices objectAtIndex:x];

        [player setVolume:volume.floatValue];
    }
}

- (void) setCallbackAndId:(CompleteCallback)cb audioId:(NSString*)aID
{
    self->audioId = aID;
    self->finished = cb;
}

- (void) audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag
{
    if (self->finished) {
        self->finished(self->audioId);
    }
}

- (void) audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error
{
    if (self->finished) {
        self->finished(self->audioId);
    }
}

@end
