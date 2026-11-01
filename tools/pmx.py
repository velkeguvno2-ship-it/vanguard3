import struct
def load(path):
    d=open(path,'rb').read()
    p=[8]
    n=d[p[0]];p[0]+=1
    g=list(d[p[0]:p[0]+n]);p[0]+=n
    enc,addUV,vi,ti,mi,bi,mo,ri=g[:8]
    def u(fmt):
        s=struct.calcsize(fmt);v=struct.unpack_from(fmt,d,p[0]);p[0]+=s;return v
    def text():
        l=u('<i')[0];b=d[p[0]:p[0]+l];p[0]+=l
        return b.decode('utf-16le' if enc==0 else 'utf8','replace')
    def idx(sz,signed=True):
        f={1:'b',2:'h',4:'i'}[sz] if signed else {1:'B',2:'H',4:'i'}[sz]
        return u('<'+f)[0]
    for _ in range(4): text()
    V=[]
    for _ in range(u('<i')[0]):
        pos=u('<3f');nor=u('<3f');uv=u('<2f')
        p[0]+=16*addUV
        wt=d[p[0]];p[0]+=1
        if wt==0: b=[idx(bi)];w=[1]
        elif wt==1: b=[idx(bi),idx(bi)];w=[u('<f')[0]];w=[w[0],1-w[0]]
        elif wt==2 or wt==4: b=[idx(bi) for _ in range(4)];w=list(u('<4f'))
        elif wt==3: b=[idx(bi),idx(bi)];w=u('<f')[0];w=[w,1-w];p[0]+=36
        p[0]+=4
        V.append((pos,nor,uv,b,w))
    fc=u('<i')[0]
    F=[idx(vi,False) for _ in range(fc)]
    tex=[text() for _ in range(u('<i')[0])]
    M=[]
    for _ in range(u('<i')[0]):
        n1=text();text()
        diff=u('<4f');p[0]+=12+4+12+1+16+4
        t=idx(ti);idx(ti);p[0]+=1
        sh=d[p[0]];p[0]+=1
        if sh==0: idx(ti)
        else: p[0]+=1
        text()
        cnt=u('<i')[0]
        M.append((n1,t,cnt,diff))
    B=[]
    for _ in range(u('<i')[0]):
        nm=text();text()
        pos=u('<3f');par=idx(bi);u('<i');fl=u('<H')[0]
        if fl&1: idx(bi)
        else: u('<3f')
        if fl&0x300: idx(bi);u('<f')
        if fl&0x400: u('<3f')
        if fl&0x800: u('<6f')
        if fl&0x2000: u('<i')
        if fl&0x20:
            idx(bi);u('<i');u('<f')
            for _ in range(u('<i')[0]):
                idx(bi);
                if u('<B')[0]: u('<6f')
        B.append((nm,pos,par))
    return dict(V=V,F=F,tex=tex,M=M,B=B)
