all: build_all;
.PHONY: all

clean: clean_all
.PHONY: clean

ifdef nogendoc
NOGENDOC=-Dnogendoc=1
else
NOGENDOC=
endif

build_all:
	ant -f ant-build.xml $(NOGENDOC)

clean_all:
	ant -d ant-build.xml clean

rebuild_all: clean_all build_all

