import Foundation
import UIKit

@objc(PitchTracker)
class PitchTracker: RCTEventEmitter {

    // MARK: Objects Handling Core Functionality
    private var modelDataHandler: ModelDataHandler? =
        ModelDataHandler(modelFileInfo: ConvActions.modelInfo)
    private var audioInputManager: AudioInputManager?
    
    // MARK: Instance Variables
    private var result: [Int]?
    private var prevKeys: [Int] = Array(repeating: 0, count: 88)
    private var bufferSize: Int = 0
    private var threshold: Int = 10

    @objc
    func start() {
        prevKeys = Array(repeating: 0, count: 88)

        guard let workingAudioInputManager = audioInputManager else {
            return
        }
        print("Audio Manager Loaded")
        
        bufferSize = workingAudioInputManager.bufferSize

        workingAudioInputManager.startTappingMicrophone()
    }

    @objc
    func stop() {
        guard let workingAudioInputManager = audioInputManager else {
            return
        }
        workingAudioInputManager.stopTappingMicrophone()
    }

    @objc
    func prepare() {
        guard let handler = modelDataHandler else {
            return
        }
        if(audioInputManager != nil) {
            return
        }
        audioInputManager = AudioInputManager(sampleRate: handler.sampleRate, sequenceLength: handler.sequenceLength)
        audioInputManager?.delegate = self
        
        guard let workingAudioInputManager = audioInputManager else {
            return
        }
        workingAudioInputManager.prepareMicrophone()
    }

    private func runModel(onBuffer buffer: [Int16]) {
        result = modelDataHandler?.runModel(onBuffer: buffer)
        guard var nowKeys = result else {
            return
        }
        for i in 0...87 {
            if(nowKeys[i] > 4) {
                sendEvent(withName: "NoteOn", body: ["midiNum": i+21])
            }
        }
    }    

    override func supportedEvents() -> [String]! {
        return ["NoteOn", "NoteOff"]
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true;
    }
}

extension PitchTracker: AudioInputManagerDelegate {
    func didOutput(channelData: [Int16]) {

        guard let handler = modelDataHandler else {
            return
        }
        bufferSize = (handler.sampleRate * handler.sequenceLength) / 1000

        self.runModel(onBuffer: Array(channelData[0..<bufferSize]))
    }
}

