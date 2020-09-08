import * as React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import PitchTracker from 'react-native-pitch-tracker';

export default function App() {
  PitchTracker.prepare();
  PitchTracker.noteOn((res) => {
    console.log('Note On: ' + res['midiNum']);
  });
  PitchTracker.noteOff((res) => {
    console.log('Note Off: ' + res['midiNum']);
  });
  PitchTracker.start();
  return (
    <View style={styles.container}>
      <Text>Application Load Complete</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
