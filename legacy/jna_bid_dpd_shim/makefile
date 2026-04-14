CC      := clang
CFLAGS  := -std=c11 -O2 -fPIC -arch x86_64

# Adjust these include paths to your local layout
INC_DEC := -I../colishaw/mth_decNumber
INC_BID := -I../intel/mth/LIBRARY/src

# Where your static libs live
LIB_DEC := ../colishaw/mth_decNumber/libdecnumber.a
LIB_BID := ../intel/mth/LIBRARY/clang000libbid.a

# Output
OUT     := libjna_bid_dpd_shim.dylib

# Objects
OBJS    := jna_bid_dpd_shim.o

all: $(OUT)

$(OUT): $(OBJS) $(LIB_DEC) $(LIB_BID)
	$(CC) -arch x86_64 -dynamiclib -o $@ $(OBJS) $(LIB_DEC) $(LIB_BID)

jna_bid_dpd_shim.o: jna_bid_dpd_shim.c
	$(CC) $(CFLAGS) $(INC_DEC) $(INC_BID) \
	      -DDECNUMDIGITS=34 \
	      -c $< -o $@

clean:
	rm -f $(OBJS) $(OUT)
