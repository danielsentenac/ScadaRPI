import fcntl, struct, ctypes, os, glob
def IOWR(nr,size): return (3<<30)|(size<<16)|(ord('d')<<8)|nr
for dev in sorted(glob.glob('/dev/dri/card*')):
    fd=os.open(dev,os.O_RDWR)
    # drm_mode_card_res: 4 u64 ptrs, 4 u32 counts, 4 u32 min/max
    fmt='QQQQIIIIIIII'
    buf=bytearray(struct.calcsize(fmt))
    fcntl.ioctl(fd,IOWR(0xA0,len(buf)),buf)
    r=struct.unpack(fmt,buf)
    ncrtc=r[5]
    arr=(ctypes.c_uint32*ncrtc)()
    buf=bytearray(struct.pack(fmt,0,ctypes.addressof(arr),0,0,0,ncrtc,0,0,0,0,0,0))
    fcntl.ioctl(fd,IOWR(0xA0,len(buf)),buf)
    for cid in arr:
        # drm_mode_crtc: u64 set_connectors_ptr,u32 count,u32 crtc_id,u32 fb_id,u32 x,u32 y,u32 gamma,u32 mode_valid, mode(68 bytes)
        mfmt='IHHHHHHHHHHIII32s'
        cfmt='QIIIIIII'+ '68s'
        b=bytearray(struct.pack(cfmt,0,0,cid,0,0,0,0,0,b''))
        fcntl.ioctl(fd,IOWR(0xA1,len(b)),b)
        c=struct.unpack(cfmt,b)
        m=struct.unpack(mfmt,c[8])
        if c[7]:
            clk,hd,hss,hse,ht,hskew,vd,vss,vse,vt,vscan,vref,flags,typ,name=m
            print(dev,'crtc',cid,'fb',c[3],name.split(b'\0')[0].decode(),'clock',clk,'kHz h',hd,hss,hse,ht,'v',vd,vss,vse,vt,'vrefresh',vref,'flags',hex(flags),'calc %.2f Hz'%(clk*1000.0/(ht*vt)))
        else:
            print(dev,'crtc',cid,'inactive')
