
Provides two tiny classes for writing `.wav` output, either as stream or
to a random-access file.

When writing to a stream, it is possible to send output without having
to provide the total size of the data stream first. In such cases, the size
of the data in the header – which cannot be altered later once the total
size is known since it has already been sent – is set to 2^32-1, the
maximum data size. While this, strictly speaking, doesn't result in a valid
file, most implementations will work normally on such files.

